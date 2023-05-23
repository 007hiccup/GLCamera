#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 texCoord;//纹理坐标，图片当中的坐标点
out vec4 outColor;

uniform samplerExternalOES s_texture;//图片，采样器

//黑白
void blackAndWhite(inout vec4 color){
    float threshold = 0.5;
    float mean = (color.r + color.g + color.b) / 3.0;
    color.r = color.g = color.b = mean >= threshold ? 1.0 : 0.0;
}

//灰度
void grey(inout vec4 color){
    float weightMean = color.r * 0.3 + color.g * 0.59 + color.b * 0.11;
    color.r = color.g = color.b = weightMean;
}

//反向
void reverse(inout vec4 color){
    color.r = 1.0 - color.r;
    color.g = 1.0 - color.g;
    color.b = 1.0 - color.b;
}

void main(){
    //四宫格滤镜
    /*float x = texCoord.x;
    float y = texCoord.y;

    if (x <= 0.5 && y <= 0.5){
        x = x * 2.0;
        y = y * 2.0;
        outColor = texture(s_texture, vec2(x, y));
        reverse(outColor);
    } else if (x <= 0.5 && y >= 0.5){
        x = x * 2.0;
        y = (y-0.5) * 2.0;
        outColor = texture(s_texture, vec2(x, y));
        blackAndWhite(outColor);
    } else if (x>0.5 &&  y > 0.5){
        x = (x-0.5) * 2.0;
        y = (y-0.5) * 2.0;
        outColor = texture(s_texture, vec2(x, y));
        grey(outColor);
    } else if (x>0.5 &&  y < 0.5){
        x = (x-0.5) * 2.0;
        y = y * 2.0;
        outColor = texture(s_texture, vec2(x, y));
    }*/

    //九宫格滤镜
    /*vec2 uv = texCoord;
    if (uv.x < 1.0 / 3.0) {
        uv.x = uv.x * 3.0;
    } else if (uv.x < 2.0 / 3.0) {
        uv.x = (uv.x - 1.0 / 3.0) * 3.0;
    } else {
        uv.x = (uv.x - 2.0 / 3.0) * 3.0;
    }
    if (uv.y <= 1.0 / 3.0) {
        uv.y = uv.y * 3.0;
    } else if (uv.y < 2.0 / 3.0) {
        uv.y = (uv.y - 1.0 / 3.0) * 3.0;
    } else {
        uv.y = (uv.y - 2.0 / 3.0) * 3.0;
    }*/

    outColor = texture(s_texture, texCoord);
}