"""Generate the public structural schema; program_validation.py also checks cross-field semantics."""
import json
from pathlib import Path

def obj(properties, required=()):
    return {'type':'object','properties':properties,'required':list(required),'additionalProperties':False}
def enum(*values):return {'enum':list(values)}
def ref(name):return {'$ref':f'#/$defs/{name}'}
def arr(item):return {'type':'array','items':item}
def nullable(value):return {'anyOf':[value,{'type':'null'}]}
num={'type':'number','minimum':0};positive={'type':'number','exclusiveMinimum':0};integer={'type':'integer','minimum':0};text={'type':'string'}
metrics=['PumpPressure','GroupPressure','BoilerPressure','PumpFlow','PumpedVolume','CupWeight','PhaseTime']
defs={}
defs['targetSource']=obj({'kind':enum('Constant','MeasurementAtEntry','CapturedValue','PreviousGoal','PreviousApplied'),'value':nullable(num),'metric':nullable(enum(*metrics[:-1])),'capture':nullable(text)},['kind'])
defs['capture']=obj({'name':{'type':'string','minLength':1},'metric':enum(*metrics[:-1]),'windowMillis':{'type':'integer','minimum':0,'maximum':10000},'maximumAgeMillis':{'type':'integer','minimum':1,'maximum':10000},'filter':enum('Latest','Mean'),'minimum':num,'maximum':num,'fallback':nullable(num)},['name','metric'])
defs['limit']=obj({'metric':enum('PumpPressure','PumpFlow'),'ceiling':positive,'minimum':nullable(num),'hysteresis':num,'reductionPerSecond':positive,'recoveryPerSecond':num,'overshootAllowance':num,'tripDurationMillis':{'type':'integer','minimum':1,'maximum':10000}},['metric','ceiling'])
defs['condition']=obj({'metric':enum(*metrics),'comparison':enum('Above','Below','StrictlyAbove','StrictlyBelow'),'threshold':num,'scope':enum('Total','Step')},['metric','comparison','threshold'])
defs['control']={'oneOf':[obj({'type':{'const':'pressure'},'bar':num,'location':enum('Pump','Group','Boiler')},['type','bar']),obj({'type':{'const':'flow'},'millilitresPerSecond':num},['type','millilitresPerSecond']),obj({'type':{'const':'pump_pause'}},['type'])]}
defs['ramp']=obj({'style':enum('Instant','Linear','EaseIn','EaseOut','EaseInOut'),'durationMillis':integer,'start':enum('PreviousTarget','CurrentMeasurement'),'source':nullable(ref('targetSource'))})
defs['phase']=obj({'id':{'type':'string','minLength':1},'name':text,'control':ref('control'),'maximumDurationMillis':{'type':'integer','minimum':1},'minimumDurationMillis':integer,'ramp':ref('ramp'),'exitConditions':arr(ref('condition')),'conditionOperator':enum('Or','And'),'adaptiveFlowProbe':nullable(obj({'samplingDurationMillis':{'type':'integer','minimum':1},'pressureCeilingBar':positive,'flowBackoffMillilitresPerSecond':num},['samplingDurationMillis','pressureCeilingBar'])),'targetSource':nullable(ref('targetSource')),'entryCaptures':arr(ref('capture')),'exitCaptures':arr(ref('capture')),'secondaryLimit':nullable(ref('limit')),'temperatureC':nullable({'type':'number','minimum':0,'maximum':130}),'stabilityDurationMillis':integer},['id','control','maximumDurationMillis'])
defs['recording']=obj({'controlMode':enum('Pressure','Flow'),'samples':{'type':'array','minItems':1,'items':obj({'elapsedMillis':integer,'data':obj({k:num for k in ('pressure','weight','volume','flowRate','weightRate')},['pressure','weight','volume','flowRate','weightRate'])},['elapsedMillis','data'])}},['controlMode','samples'])
defs['source']=obj({'format':text,'title':text,'author':text,'url':text,'sha256':{'type':'string','pattern':'^[a-fA-F0-9]{64}$'},'original':{'type':'object'}},['format','title','sha256','original'])
defs['recipe']=obj({'templateId':text,'source':ref('source'),'templatePhases':arr(ref('phase')),'settings':obj({k:nullable(positive) for k in ('doseG','ratio','targetYieldG')}),'diagnostics':arr(text),'adaptation':nullable(text),'unresolvedSemantics':arr(text)},['templateId','source','templatePhases'])
defs['execution']=obj({'maximumTotalDurationMillis':{'type':'integer','minimum':1,'maximum':40000}})
common={'version':enum(1,2),'execution':ref('execution')}
defs['program']={'oneOf':[obj({**common,'type':{'const':'phases'},'phases':{'type':'array','minItems':1,'items':ref('phase')},'recipe':nullable(ref('recipe'))},['type','phases']),obj({**common,'type':{'const':'recording'},'recording':ref('recording'),'originalRecording':nullable(ref('recording')),'replayPolicy':enum('Legacy','Hold','Linear')},['type','recording'])]}
defs['profile']=obj({'name':{'type':'string','minLength':1},'description':text,'finishCondition':nullable(obj({'type':enum('weight','volume'),'target':positive},['type','target'])),'autoLinkOpen':{'type':'boolean'},'singleDoseSettings':obj({'enabled':{'type':'boolean'},'grindingSize':{'type':'integer','minimum':0,'maximum':600},'grindingSpeed':{'type':'integer','minimum':200,'maximum':1000}},['enabled','grindingSize','grindingSpeed']),'program':ref('program')},['name','description','autoLinkOpen','singleDoseSettings','program'])
defs['record']=obj({'id':{'type':'string','format':'uuid'},'revision':integer,'favorite':{'type':'boolean'},'deleted':{'type':'boolean'},'profile':ref('profile')},['id','revision','favorite','deleted','profile'])
schema={'$schema':'https://json-schema.org/draft/2020-12/schema','$id':'https://hostinghk.pangxinlabi.top/profile-library/catalog.schema.json','title':'GeeFlow profile library v1/v2',**obj({'schemaVersion':enum(1,2),'revision':integer,'profiles':arr(ref('record'))},['schemaVersion','revision','profiles']),'$defs':defs}
Path(__file__).with_name('catalog.schema.json').write_text(json.dumps(schema,indent=2)+'\n')
