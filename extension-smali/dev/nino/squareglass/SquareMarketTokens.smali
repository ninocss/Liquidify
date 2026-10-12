# Square Glass for TIDAL. GPL-3.0-or-later.
.class public final Ldev/nino/squareglass/SquareMarketTokens;
.super Ljava/lang/Object;

.method public static select(Lv20/i$b;)Lv20/i$b;
    .locals 1

    invoke-static {}, Ldev/nino/squareglass/SquarePalette;->isDark()Z
    move-result v0
    if-eqz v0, :light
    sget-object v0, Ly20/e;->a:Ly20/e;
    return-object v0
    :light
    sget-object v0, Ly20/f;->a:Ly20/f;
    return-object v0
.end method
