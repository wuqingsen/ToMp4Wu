precision mediump float;
varying vec2 ft_Position;
uniform sampler2D sampler_y;
uniform sampler2D sampler_u;
uniform sampler2D sampler_v;
void main() {
    float y,u,v;
    y = texture2D(sampler_y,ft_Position).x;
    u = texture2D(sampler_u,ft_Position).x- 128./255.;
    v = texture2D(sampler_v,ft_Position).x- 128./255.;

    vec3 rgb;
    rgb.r = y + 1.403 * v;
    rgb.g = y - 0.344 * u - 0.714 * v;
    rgb.b = y + 1.770 * u;

    gl_FragColor = vec4(rgb,1);
}
