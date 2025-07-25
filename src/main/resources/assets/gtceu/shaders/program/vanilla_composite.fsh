#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HighlightSampler;
uniform sampler2D BlurTexture;
uniform float BloomStrength;
uniform float BaseBrightness;
uniform float MaxBrightness;
uniform float MinBrightness;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 bloom = texture(BlurTexture, texCoord).rgb * BloomStrength;
    vec4 background = texture(DiffuseSampler, texCoord);
    vec4 highLight = texture(HighlightSampler, texCoord);
    background.rgb = background.rgb * (1 - highLight.a) + highLight.a * highLight.rgb;
    float max = max(background.b, max(background.r, background.g));
    float min = min(background.b, min(background.r, background.g));
    fragColor = vec4(background.rgb + bloom.rgb * ((1. - (max + min) / 2.) * (MaxBrightness - MinBrightness) + MinBrightness + BaseBrightness), 1.);
}
