#include <metal_stdlib>
#include <SwiftUI/SwiftUI_Metal.h>
using namespace metal;

/// Recolors "sentinel" ink (pure white, any alpha) per pixel from the backdrop luminance behind it,
/// so a line of text can turn dark where it crosses a bright sky and light again over the sea.
/// Every other color (yellow accents, photos, card fills) passes through untouched.
///
/// `lum` is a columns x rows grid of the composited backdrop luminance in screen space;
/// `origin` is the layer's top-left in that space, so the lookup follows scrolling.
[[ stitchable ]] half4 adaptiveInk(
    float2 position,
    half4 color,
    float2 origin,
    float2 canvas,
    device const float *lum,
    int count,
    float columns,
    float rows,
    half4 lightInk,
    half4 darkInk,
    float surface,
    float crossover
) {
    if (color.a < 0.004h) {
        return color;
    }
    half3 rgb = color.rgb / color.a;
    if (min(rgb.r, min(rgb.g, rgb.b)) < 0.985h) {
        return color;
    }
    int cols = int(columns);
    int rws = int(rows);
    if (count < cols * rws || cols < 1 || rws < 1) {
        return color;
    }

    // Bilinear lookup between cell centres keeps the color boundary soft and moving smoothly.
    float2 uv = clamp((origin + position) / canvas, 0.0, 1.0);
    float x = clamp(uv.x * columns - 0.5, 0.0, columns - 1.0);
    float y = clamp(uv.y * rows - 0.5, 0.0, rows - 1.0);
    int x0 = int(floor(x));
    int y0 = int(floor(y));
    int x1 = min(x0 + 1, cols - 1);
    int y1 = min(y0 + 1, rws - 1);
    float fx = x - float(x0);
    float fy = y - float(y0);
    float top = mix(lum[y0 * cols + x0], lum[y0 * cols + x1], fx);
    float bottom = mix(lum[y1 * cols + x0], lum[y1 * cols + x1], fx);

    // A translucent black surface (a card) between the photo and the text darkens what it reads against.
    float luminance = mix(top, bottom, fy) * (1.0 - surface);
    half toDark = half(smoothstep(crossover - 0.03, crossover + 0.03, luminance));
    half3 ink = mix(lightInk.rgb, darkInk.rgb, toDark);
    return half4(ink * color.a, color.a);
}
