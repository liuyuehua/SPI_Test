#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES camTex;
uniform sampler2D bitmapTex;

varying vec2 camTexCoordinate;

void main () {
    vec4 color = texture2D(camTex, camTexCoordinate);
    vec4 pixel = texture2D(bitmapTex, camTexCoordinate);
    float factor = pixel.a;
    gl_FragColor = vec4(color*(1.0f-factor))+ vec4(pixel*factor);
}
