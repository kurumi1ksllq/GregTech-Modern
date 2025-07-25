#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HighlightSampler;
uniform sampler2D BlurTexture1;
uniform sampler2D BlurTexture2;
uniform sampler2D BlurTexture3;
uniform sampler2D BlurTexture4;
uniform float BloomRadius;
uniform float BloomStrength;
//uniform float BaseBrightness;
//uniform float MaxBrightness;
//uniform float MinBrightness;

in vec2 texCoord;
out vec4 fragColor;

float lerpBloomFactor(float factor) {
    float mirrorFactor = 1.2 - factor;
    return mix(factor, mirrorFactor, BloomRadius);
}

vec3 jodieReinhardTonemap(vec3 c) {
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 tc = c / (c + 1.0);

    return mix(c / (l + 1.0), tc, tc);
}

void main() {
    vec4 bloom = BloomStrength * (
        lerpBloomFactor(1.0) * texture(BlurTexture1, texCoord) +
        lerpBloomFactor(0.8) * texture(BlurTexture2, texCoord) +
        lerpBloomFactor(0.6) * texture(BlurTexture3, texCoord) +
        lerpBloomFactor(0.4) * texture(BlurTexture4, texCoord));

    vec4 background = texture(DiffuseSampler, texCoord);
    vec4 highlight = texture(HighlightSampler, texCoord);
    background.rgb = background.rgb * (1 - highlight.a) + highlight.a * highlight.rgb;
    fragColor = vec4(background.rgb + jodieReinhardTonemap(bloom.rgb), 1.0);
}
