"""Portable program validation. Machine compatibility is assessed separately by GeeFlow."""
import math
import re
METRICS = {'PumpPressure','GroupPressure','BoilerPressure','PumpFlow','PumpedVolume','CupWeight','PhaseTime'}
STYLES = {'Instant','Linear','EaseIn','EaseOut','EaseInOut'}

def check(condition, message):
    if not condition: raise ValueError(message)

def number(value, minimum=0, maximum=float('inf')):
    return isinstance(value, (int,float)) and not isinstance(value,bool) and math.isfinite(value) and minimum <= value <= maximum

def integer(value, minimum=0, maximum=9007199254740991):
    return isinstance(value,int) and not isinstance(value,bool) and minimum <= value <= maximum

def fields(obj, names, label):
    check(isinstance(obj,dict), f'{label} must be an object')
    check(not (set(obj)-set(names.split())), f'Unknown {label} fields')

def source(value, available):
    if value is None: return
    fields(value, 'kind value metric capture', 'target source')
    kind=value.get('kind')
    check(kind in {'Constant','MeasurementAtEntry','CapturedValue','PreviousGoal','PreviousApplied'}, 'Unknown target source')
    if kind=='Constant': check(number(value.get('value')), 'Invalid source constant')
    if kind=='MeasurementAtEntry': check(value.get('metric') in METRICS-{'PhaseTime'}, 'Invalid measurement source')
    if kind=='CapturedValue': check(value.get('capture') in available, 'Capture must precede its use')

def validate_recording(recording):
    fields(recording,'controlMode samples','recording')
    check(recording.get('controlMode') in {'Pressure','Flow'}, 'Invalid recording mode')
    samples=recording.get('samples')
    check(isinstance(samples,list) and len(samples)>0,'Recording needs samples')
    previous=-1
    for sample in samples:
        fields(sample,'elapsedMillis data','sample')
        t=sample.get('elapsedMillis')
        check(integer(t) and t>previous and (previous!=-1 or t==0),'Sample times must start at zero and increase')
        previous=t
        data=sample.get('data');fields(data,'pressure weight volume flowRate weightRate','sample data')
        check(all(number(data.get(k)) for k in ('pressure','weight','volume','flowRate','weightRate')),'Invalid sample measurement')

def validate_program(program):
    fields(program,'type version phases execution recipe recording originalRecording replayPolicy','program')
    version=program.get('version',1)
    check(integer(version,1,2),'Unsupported program version')
    policy=program.get('execution',{})
    fields(policy,'maximumTotalDurationMillis','execution policy')
    check(integer(policy.get('maximumTotalDurationMillis',40000),1,40000),'Live deadline must be at most 40 seconds')
    check(version==2 or policy.get('maximumTotalDurationMillis',40000)==40000,'Custom deadline requires v2')
    if program.get('type')=='recording':
        check('phases' not in program and 'recipe' not in program,'Invalid recording program fields')
        validate_recording(program.get('recording'))
        if program.get('originalRecording') is not None:
            check(version==2,'Original recording requires v2');validate_recording(program['originalRecording'])
        check(program.get('replayPolicy','Legacy') in ({'Legacy'} if version==1 else {'Legacy','Hold','Linear'}),'Unsupported replay policy')
        return
    check(program.get('type')=='phases','Unsupported program type')
    check(not any(k in program for k in ('recording','originalRecording','replayPolicy')),'Invalid phase program fields')
    phases=program.get('phases');check(isinstance(phases,list) and phases,'Program needs a phase')
    ids=set();available=set()
    for phase in phases:
        fields(phase,'id name control maximumDurationMillis minimumDurationMillis ramp exitConditions conditionOperator adaptiveFlowProbe targetSource entryCaptures exitCaptures secondaryLimit temperatureC stabilityDurationMillis','phase')
        ident=phase.get('id');check(isinstance(ident,str) and ident and ident not in ids,'Phase IDs must be unique');ids.add(ident)
        maximum=phase.get('maximumDurationMillis');minimum=phase.get('minimumDurationMillis',0)
        check(integer(maximum,1) and integer(minimum,0,maximum),'Invalid phase duration')
        control=phase.get('control');check(isinstance(control,dict),'Control must be an object');fields(control,'type bar location' if control.get('type')=='pressure' else 'type millilitresPerSecond' if control.get('type')=='flow' else 'type','control')
        mode=control.get('type');check(mode in {'pressure','flow','pump_pause'},'Invalid control mode')
        if mode=='pressure': check(number(control.get('bar')) and control.get('location','Pump') in {'Pump','Group','Boiler'},'Invalid pressure target')
        if mode=='flow': check(number(control.get('millilitresPerSecond')),'Invalid flow target')
        ramp=phase.get('ramp',{});fields(ramp,'style durationMillis start source','ramp')
        check(ramp.get('style','Instant') in STYLES and integer(ramp.get('durationMillis',0),0,maximum) and ramp.get('start','PreviousTarget') in {'PreviousTarget','CurrentMeasurement'},'Invalid ramp')
        check(mode!='pump_pause' or ramp.get('style','Instant')=='Instant','Pause cannot ramp')
        check(phase.get('conditionOperator','Or') in {'Or','And'},'Invalid condition operator')
        check(isinstance(phase.get('name',''),str),'Phase name must be text')
        check(isinstance(phase.get('exitConditions',[]),list),'Exit conditions must be a list')
        for condition in phase.get('exitConditions',[]):
            fields(condition,'metric comparison threshold scope','condition')
            check(condition.get('metric') in METRICS and condition.get('comparison') in {'Above','Below','StrictlyAbove','StrictlyBelow'} and number(condition.get('threshold')),'Invalid exit condition')
            check(condition.get('scope','Total')=='Total' or condition.get('scope')=='Step' and condition['metric'] in {'PumpedVolume','CupWeight'},'Invalid condition scope')
        probe=phase.get('adaptiveFlowProbe')
        if probe is not None:
            fields(probe,'samplingDurationMillis pressureCeilingBar flowBackoffMillilitresPerSecond','probe')
            check(mode=='flow' and integer(probe.get('samplingDurationMillis'),1,maximum) and number(probe.get('pressureCeilingBar'),0.0001) and number(probe.get('flowBackoffMillilitresPerSecond',0.1)),'Invalid adaptive probe')
            check(ramp.get('style','Instant')!='Instant' and ramp.get('durationMillis',0)>=probe['samplingDurationMillis'],'Probe ramp must cover sampling window')
        extended=any(phase.get(k) for k in ('targetSource','entryCaptures','exitCaptures','secondaryLimit','temperatureC','stabilityDurationMillis')) or ramp.get('source') is not None or phase.get('temperatureC') is not None
        check(version==2 or not extended,'Extended phase fields require program v2')
        check(integer(phase.get('stabilityDurationMillis',0),0,maximum),'Invalid stability duration')
        check(phase.get('temperatureC') is None or number(phase['temperatureC'],0,130),'Invalid temperature')
        def captures(actions):
            check(isinstance(actions,list),'Captures must be a list')
            for action in actions:
                fields(action,'name metric windowMillis maximumAgeMillis filter minimum maximum fallback','capture')
                name=action.get('name');check(isinstance(name,str) and name.strip() and name not in available,'Duplicate or empty capture')
                check(action.get('metric') in METRICS-{'PhaseTime'} and action.get('filter','Mean') in {'Mean','Latest'},'Invalid capture source')
                check(integer(action.get('windowMillis',500),0,10000) and integer(action.get('maximumAgeMillis',500),1,10000),'Invalid capture timing')
                lo=action.get('minimum',0);hi=action.get('maximum',12)
                check(number(lo) and number(hi,lo),'Invalid capture bounds')
                check(action.get('fallback') is None or number(action['fallback'],lo,hi),'Invalid capture fallback')
                available.add(name)
        captures(phase.get('entryCaptures',[]));source(phase.get('targetSource'),available);source(ramp.get('source'),available);captures(phase.get('exitCaptures',[]))
        limit=phase.get('secondaryLimit')
        if limit is not None:
            fields(limit,'metric ceiling minimum hysteresis reductionPerSecond recoveryPerSecond overshootAllowance tripDurationMillis','limit')
            check((mode=='flow' and limit.get('metric')=='PumpPressure') or (mode=='pressure' and limit.get('metric')=='PumpFlow'),'Limit must constrain the other control metric')
            ceiling=limit.get('ceiling');check(number(ceiling,0.0001) and number(limit.get('hysteresis',0.2),0,ceiling) and limit.get('hysteresis',0.2)<ceiling,'Invalid limit ceiling')
            check(limit.get('minimum') is None or number(limit['minimum']) and limit['minimum']<ceiling,'Range minimum must be below maximum')
            check(number(limit.get('reductionPerSecond',1),0.0001) and number(limit.get('recoveryPerSecond',0.25)) and number(limit.get('overshootAllowance',1)) and integer(limit.get('tripDurationMillis',1000),1,10000),'Invalid limit response')
    recipe=program.get('recipe')
    if recipe is not None:
        check(version==2,'Recipe metadata requires v2')
        fields(recipe,'templateId source templatePhases settings diagnostics adaptation unresolvedSemantics','recipe')
        check(isinstance(recipe.get('templateId'),str) and recipe['templateId'].strip(),'Recipe template ID required')
        check(recipe.get('adaptation') is None or isinstance(recipe['adaptation'],str),'Invalid adaptation')
        for key in ('diagnostics','unresolvedSemantics'):
            check(isinstance(recipe.get(key,[]),list) and all(isinstance(x,str) for x in recipe.get(key,[])),'Recipe reports must be text lists')
        origin=recipe.get('source');fields(origin,'format title author url sha256 original','recipe source')
        check(isinstance(origin.get('original'),dict) and isinstance(origin.get('sha256'),str) and re.fullmatch('[a-fA-F0-9]{64}',origin['sha256']) is not None,'Original source and SHA-256 required')
        check(all(isinstance(origin.get(key,''),str) for key in ('format','title','author','url')) and origin.get('format','').strip(),'Invalid source metadata')
        validate_program({'type':'phases','version':2,'phases':recipe.get('templatePhases')})
        settings=recipe.get('settings',{});fields(settings,'doseG ratio targetYieldG','brew settings')
        check(all(v is None or number(v,0.0001) for v in settings.values()),'Brew settings must be positive')
