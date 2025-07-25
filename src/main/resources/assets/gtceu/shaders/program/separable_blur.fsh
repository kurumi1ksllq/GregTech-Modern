#version 150

#define PI 3.1415926538

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

vec2 outTexel = 1.0 / OutSize;

float calcExpectedMean() {
    float invRadius = 1.0 / Radius;
    float result = 0.0;
    for (float i = 1; i <= Radius; i += 1) {
        result += i * invRadius;
    }
    return result;
}

float variance = ((Radius * Radius) - 1) / 12;
float twoVariance = 2.0 * variance;
float expectedMean = calcExpectedMean();

// pdf stands for "Probability density function"
float gaussianPdf(float x) {
    return 1.0 / sqrt(PI * twoVariance) * exp(-pow(x - expectedMean, 2.0) / twoVariance);
}

void main() {
    vec4 blurred = vec4(0.0);
    float totalAlpha = 0.0;
    float totalSamples = 1.0;

    for(float r = 0; r <= Radius; r += 1.0) {
        vec2 uvOffset = outTexel * r * BlurDir;
        vec4 sample1 = texture(DiffuseSampler, texCoord + uvOffset);
        vec4 sample2 = texture(DiffuseSampler, texCoord - uvOffset);

        float weight = gaussianPdf(r);
        vec4 sampleValue = (sample1 + sample2) * weight;

        // Accumulate average alpha & smoothed blur
        totalAlpha += sampleValue.a;
        totalSamples += 2.0 * weight;
        blurred += sampleValue;
    }
    fragColor = vec4(blurred.rgb / totalSamples, totalAlpha);
}
