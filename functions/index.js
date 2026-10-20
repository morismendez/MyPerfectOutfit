const functions = require("firebase-functions/v1");
const axios = require("axios");

const SECRETS = ["AZURE_AI_KEY", "AZURE_AI_ENDPOINT", "AZURE_AI_DEPLOYMENT"];

async function postWithTokenFallback(url, headers, basePayload, tokenLimit) {
    // Try max_completion_tokens first
    let payload = { ...basePayload, max_completion_tokens: tokenLimit };
    try {
        const res = await axios.post(url, payload, { headers });
        return res;
    } catch (error) {
        const msg = error.response?.data?.error?.message || error.message || "";
        if (msg.includes("max_completion_tokens") || msg.includes("max_tokens") || msg.includes("Unsupported parameter")) {
            // Fall back to max_tokens
            delete payload.max_completion_tokens;
            payload.max_tokens = tokenLimit;
            return await axios.post(url, payload, { headers });
        }
        throw error;
    }
}

exports.getOutfitRecommendation = functions.runWith({
    secrets: SECRETS,
    timeoutSeconds: 60,
    memory: '512MB'
}).https.onCall(async (data, context) => {
    if (!context.app) {
        console.warn("App Check: Petición recibida sin verificación de App Check activa (Modo de pruebas/desarrollo).");
    }

    const apiKey = process.env.AZURE_AI_KEY;
    const deployment = process.env.AZURE_AI_DEPLOYMENT;
    let endpoint = process.env.AZURE_AI_ENDPOINT.trim();

    if (endpoint.endsWith("/")) endpoint = endpoint.slice(0, -1);
    const url = `${endpoint}/chat/completions`;

    try {
        const headers = { "api-key": apiKey, "Content-Type": "application/json" };
        const basePayload = {
            model: deployment,
            messages: [
                { role: "system", content: data.systemInstruction },
                { role: "user", content: data.userPrompt }
            ]
        };

        const response = await postWithTokenFallback(url, headers, basePayload, 2500);
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
    if (!context.app) {
        console.warn("App Check: Petición recibida sin verificación de App Check activa (Modo de pruebas/desarrollo).");
    }

    const apiKey = process.env.AZURE_AI_KEY;
    const deployment = process.env.AZURE_AI_DEPLOYMENT;
    let endpoint = process.env.AZURE_AI_ENDPOINT.trim();

    if (endpoint.endsWith("/")) endpoint = endpoint.slice(0, -1);
    const url = `${endpoint}/chat/completions`;

    try {
        const headers = { "api-key": apiKey, "Content-Type": "application/json" };
        const basePayload = {
            model: deployment,
            messages: [
                { role: "system", content: data.systemInstruction },
                {
                    role: "user",
                    content: [
                        { type: "text", text: data.userPrompt },
                        { type: "image_url", image_url: { url: data.image } }
                    ]
                }
            ]
        };

        const response = await postWithTokenFallback(url, headers, basePayload, 1500);
        return response.data.choices[0].message.content;
    } catch (error) {
        const detail = error.response?.data?.error?.message || error.message;
        throw new functions.https.HttpsError("internal", detail);
    }
});
