.class public final Lradiant/WordLyricsTick;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

# 50ms clock tick driver for word level lyrics highlighting

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public run()V
    .locals 0

    invoke-static {}, Lradiant/WordLyrics;->tick()V

    return-void
.end method
