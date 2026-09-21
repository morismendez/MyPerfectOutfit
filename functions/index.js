const functions = require("firebase-functions");
const axios = require("axios");

const SECRETS = ["AZURE_AI_KEY", "AZURE_AI_ENDPOINT", "AZURE_AI_DEPLOYMENT"];

exports.getOutfitRecommendation = functions.runWith({
    secrets: SECRETS,
    timeoutSeconds: 60,
    memory: '512MB'
}).https.onCall(async (data, context) => {
    const apiKey = process.env.AZURE_AI_KEY;
    const deployment = process.env.AZURE_AI_DEPLOYMENT;
    let endpoint = process.env.AZURE_AI_ENDPOINT.trim();

    if (endpoint.endsWith("/")) endpoint = endpoint.slice(0, -1);
    const url = `${endpoint}/chat/completions`;

    try {
        const response = await axios.post(url, {
            model: deployment,
            messages: [
                { role: "user", content: `${data.systemInstruction}\n\n${data.userPrompt}` }
            ],
            max_completion_tokens: 2500
        }, {
            headers: { "api-key": apiKey, "Content-Type": "application/json" }
        });

        return response.data.choices[0].message.content;
    } catch (error) {
        const detail = error.response?.data?.error?.message || error.message;
        throw new functions.https.HttpsError("internal", detail);
    }
});

exports.analyzeGarment = functions.runWith({
    secrets: SECRETS,
    timeoutSeconds: 60,
    memory: '512MB'
}).https.onCall(async (data, context) => {
    const apiKey = process.env.AZURE_AI_KEY;
    const deployment = process.env.AZURE_AI_DEPLOYMENT;
    let endpoint = process.env.AZURE_AI_ENDPOINT.trim();

    if (endpoint.endsWith("/")) endpoint = endpoint.slice(0, -1);
    const url = `${endpoint}/chat/completions`;

    try {
        const response = await axios.post(url, {
            model: deployment,
            messages: [
                {
                    role: "user",
                    content: [
                        { type: "text", text: `${data.systemInstruction}\n\n${data.userPrompt}` },
                        { type: "image_url", image_url: { url: data.image } }
                    ]
                }
            ],
            max_completion_tokens: 1500
        }, {
            headers: { "api-key": apiKey, "Content-Type": "application/json" }
        });

        return response.data.choices[0].message.content;
    } catch (error) {
        const detail = error.response?.data?.error?.message || error.message;
        throw new functions.https.HttpsError("internal", detail);
    }
});
