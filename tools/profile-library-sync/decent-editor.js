const newStage = () => ({
  name: "New stage", temperature: "92", sensor: "coffee", pump: "pressure",
  transition: "fast", pressure: "3", flow: "0", seconds: "5", volume: "0",
  weight: "0", limiter: {value: "0", range: "0.6"},
});

let recipe = {
  title: "New recipe", author: "", notes: "", beverage_type: "espresso",
  steps: [newStage()], tank_temperature: "0", target_weight: "36", target_volume: "0",
  target_volume_count_start: "0", legacy_profile_type: "settings_2c", type: "advanced",
  lang: "en", hidden: "0", reference_file: "", changes_since_last_espresso: "", version: "2",
};

const byId = id => document.getElementById(id);
function field(parent, object, key, label, options, refresh) {
  const wrapper = document.createElement("label");
  wrapper.append(label);
  const control = document.createElement(options ? "select" : key === "notes" ? "textarea" : "input");
  if (options) {
    for (const option of new Set([...options, object[key] ?? options[0]])) {
      control.add(new Option(option, option));
    }
  }
  control.value = object[key] ?? "";
  control.addEventListener("input", () => { object[key] = control.value; });
  if (refresh) control.addEventListener("change", refresh);
  wrapper.append(control);
  parent.append(wrapper);
}

function button(parent, title, handler, disabled = false) {
  const element = document.createElement("button");
  element.textContent = title;
  element.disabled = disabled;
  element.onclick = handler;
  parent.append(element);
}

function group(parent, name) {
  const element = document.createElement("fieldset");
  const title = document.createElement("legend");
  title.textContent = name;
  const fields = document.createElement("section");
  element.append(title, fields);
  parent.append(element);
  return fields;
}

function render(openIndex = 0) {
  const profile = byId("profile");
  profile.replaceChildren();
  for (const [key, label] of [
    ["title", "Profile name"], ["author", "Author"], ["notes", "Notes"],
    ["target_weight", "Target weight (g)"], ["target_volume", "Target volume (mL)"],
    ["target_volume_count_start", "Volume counting starts at step"], ["tank_temperature", "Tank temperature (°C)"],
  ]) field(profile, recipe, key, label);
  const stages = byId("stages");
  stages.replaceChildren();
  recipe.steps.forEach((step, index) => {
    const refresh = () => render(index);
    const card = document.createElement("details");
    card.open = index === openIndex;
    const summary = document.createElement("summary");
    const pressure = step.pump === "pressure";
    summary.textContent = `${index + 1}. ${step.name} · ${pressure ? step.pressure + " bar" : step.flow + " mL/s"} · ${step.seconds} s`;
    card.append(summary);
    field(card, step, "name", "Stage name");
    const temp = group(card, "Temperature");
    field(temp, step, "temperature", "Target (°C)");
    field(temp, step, "sensor", "Sensor", ["coffee", "water"]);
    const pump = group(card, "Pump");
    field(pump, step, "pump", "Pump control", ["pressure", "flow"], refresh);
    field(pump, step, "pressure", pressure ? "Active target · Pressure (bar)" : "Pressure (bar)");
    field(pump, step, "flow", pressure ? "Flow (mL/s)" : "Active target · Flow (mL/s)");
    field(pump, step, "transition", "Transition", ["fast", "smooth"]);
    const maximum = group(card, "Maximum");
    field(maximum, step, "seconds", "Time (s)");
    field(maximum, step, "volume", "Volume (mL)");
    field(maximum, step, "weight", "Weight (g)");
    const exit = group(card, "Move on if");
    if (step.exit) {
      field(exit, step.exit, "type", "Measurement", ["pressure", "flow"], refresh);
      field(exit, step.exit, "condition", "Comparison", ["over", "under"]);
      field(exit, step.exit, "value", step.exit.type === "pressure" ? "Threshold (bar)" : "Threshold (mL/s)");
      button(exit, "Remove exit", () => { delete step.exit; refresh(); });
    } else {
      button(exit, "Add exit", () => { step.exit = {type: "pressure", condition: "over", value: "3"}; refresh(); });
    }
    const limiter = group(card, "Limiter");
    const unit = pressure ? "mL/s" : "bar";
    if (step.limiter) {
      field(limiter, step.limiter, "value", `Limit (${unit})`);
      field(limiter, step.limiter, "range", `Range (${unit})`);
    } else {
      button(limiter, "Add limiter", () => { step.limiter = {value: "0", range: "0.6"}; refresh(); });
    }
    function move(destination) {
      recipe.steps.splice(destination, 0, recipe.steps.splice(index, 1)[0]);
      render(destination);
    }
    button(card, "Move up", () => move(index - 1), index === 0);
    button(card, "Move down", () => move(index + 1), index === recipe.steps.length - 1);
    button(card, "Remove stage", () => { recipe.steps.splice(index, 1); render(); }, recipe.steps.length === 1);
    stages.append(card);
  });
}

byId("add").onclick = () => { recipe.steps.push(newStage()); render(recipe.steps.length - 1); };
byId("import").onclick = () => byId("file").click();
byId("file").onchange = async event => {
  try {
    const file = event.target.files[0];
    if (!file) return;
    if (file.size > 2_000_000) throw new Error("Recipe file exceeds 2 MB");
    const parsed = JSON.parse(await file.text());
    if (parsed?.version !== "2" || !Array.isArray(parsed.steps) || !parsed.steps.every(step => step && typeof step === "object" && !Array.isArray(step))) {
      throw new Error("Select a Decent JSON v2 profile");
    }
    recipe = parsed;
    render();
    byId("status").textContent = "Imported Decent JSON v2";
  } catch (error) {
    byId("status").textContent = error.message;
  } finally {
    event.target.value = "";
  }
};
byId("export").onclick = () => {
  const url = URL.createObjectURL(new Blob([JSON.stringify(recipe, null, 2) + "\n"], {type: "application/json"}));
  const link = document.createElement("a");
  link.href = url;
  link.download = "decent-profile.json";
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};
render();
