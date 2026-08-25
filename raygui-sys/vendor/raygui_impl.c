/*
 * The single compilation unit for raygui.
 *
 * raygui is a header-only library: declarations and implementation live in the
 * same file, and RAYGUI_IMPLEMENTATION must be defined in exactly ONE
 * translation unit. That makes this file the whole of the C in this project.
 *
 * Vendored revision: raygui 5.0-9-gfbf5d95 (see NOTICE).
 *
 * Built by jank-build.bb into libraygui.dylib (or .so on Linux), linked
 * DYNAMICALLY against the libraylib that the raylib-sys package built. The
 * dynamic link against THAT libraylib is load-bearing: raygui's controls call
 * GetMousePosition, DrawRectangle and friends internally, and those must
 * resolve to the same libraylib instance jank itself links. Against a second
 * copy every control reads empty input state and is inert, with no error to
 * show for it.
 */
#define RAYGUI_IMPLEMENTATION
#include "raygui.h"
