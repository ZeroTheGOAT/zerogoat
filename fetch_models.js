const fs = require('fs');
const https = require('https');

https.get('https://openrouter.ai/api/v1/models', (res) => {
  let data = '';

  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    const models = JSON.parse(data).data;
    
    // Group models by a derived provider from ID
    // OpenRouter IDs are typically "provider/model-name"
    
    let output = '';
    const sortedModels = models.sort((a, b) => a.id.localeCompare(b.id));
    
    for (const model of sortedModels) {
      if (!model.id || !model.name) continue;
      
      const id = model.id;
      const name = model.name.replace(/"/g, '\\"');
      const providerId = id.split('/')[0] || "Unknown";
      
      // Capitalize provider
      const provider = providerId.charAt(0).toUpperCase() + providerId.slice(1);
      
      // Guess category based on name/id
      let category = "Category.SMART";
      if (model.pricing && model.pricing.prompt === "0" && model.pricing.completion === "0") {
          category = "Category.FREE";
      } else if (id.includes("vision") || name.toLowerCase().includes("vision")) {
          category = "Category.VISION";
      } else if (id.includes("coder") || name.toLowerCase().includes("code")) {
          category = "Category.CODING";
      } else if (id.includes("8b") || id.includes("7b") || id.includes("mini") || id.includes("flash") || id.includes("haiku")) {
          category = "Category.FAST";
      } else if (id.includes("r1") || id.includes("o1") || id.includes("o3") || id.includes("thinking") || id.includes("reasoning")) {
          category = "Category.REASONING";
      }
      
      const contextLength = model.context_length || 128000;
      const supportsVision = category === "Category.VISION" ? "true" : "false"; // Heuristic
      
      // Calculate costs per 1M tokens
      let inputCost = 0.0;
      let outputCost = 0.0;
      if (model.pricing) {
          inputCost = (parseFloat(model.pricing.prompt) * 1000000).toFixed(4);
          outputCost = (parseFloat(model.pricing.completion) * 1000000).toFixed(4);
      }
      
      const isFree = category === "Category.FREE" ? "true" : "false";
      
      output += `        ModelInfo("${id}", "${name}", "${provider}", ${category}, ${contextLength}, supportsVision = ${supportsVision}, inputCostPer1M = ${inputCost}, outputCostPer1M = ${outputCost}${isFree === "true" ? ", isFree = true" : ""}),\n`;
    }
    
    fs.writeFileSync('C:/project/zerogoat/models.txt', output);
    console.log(`Wrote ${sortedModels.length} models to models.txt`);
  });

}).on("error", (err) => {
  console.log("Error: " + err.message);
});
