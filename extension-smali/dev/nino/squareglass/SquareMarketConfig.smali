# Square Glass for TIDAL. GPL-3.0-or-later.
.class public final Ldev/nino/squareglass/SquareMarketConfig;
.super Ljava/lang/Object;

.method public static create()Lm20/b;
    .locals 4

    invoke-static {}, Ldev/nino/squareglass/SquareSettings;->themeMode()I
    move-result v0
    if-eqz v0, :system
    const/4 v1, 0x2
    if-ne v0, v1, :light
    sget-object v0, Lcom/squareup/ui/market/designtokens/core/ColorMode;->DARK:Lcom/squareup/ui/market/designtokens/core/ColorMode;
    goto :explicit
    :light
    sget-object v0, Lcom/squareup/ui/market/designtokens/core/ColorMode;->LIGHT:Lcom/squareup/ui/market/designtokens/core/ColorMode;
    :explicit
    new-instance v1, Lm20/a$a;
    invoke-direct {v1, v0}, Lm20/a$a;-><init>(Lcom/squareup/ui/market/designtokens/core/ColorMode;)V
    goto :config
    :system
    sget-object v1, Lm20/a$b;->b:Lm20/a$b;
    :config
    new-instance v0, Lm20/b;
    sget-object v2, Lm20/e$e;->a:Lm20/e$e;
    const/4 v3, 0x0
    invoke-direct {v0, v1, v2, v3}, Lm20/b;-><init>(Lm20/a;Lm20/e;Z)V
    return-object v0
.end method
