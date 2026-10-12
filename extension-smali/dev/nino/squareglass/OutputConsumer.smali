.class public final Ldev/nino/squareglass/OutputConsumer;
.super Ljava/lang/Object;
.implements Lio/reactivex/functions/Consumer;
.field private final callback:Ljava/lang/Object;
.field private final error:Z
.method public constructor <init>(Ljava/lang/Object;Z)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Ldev/nino/squareglass/OutputConsumer;->callback:Ljava/lang/Object;
    iput-boolean p2, p0, Ldev/nino/squareglass/OutputConsumer;->error:Z
    return-void
.end method
.method public accept(Ljava/lang/Object;)V
    .locals 2
    iget-object v0, p0, Ldev/nino/squareglass/OutputConsumer;->callback:Ljava/lang/Object;
    iget-boolean v1, p0, Ldev/nino/squareglass/OutputConsumer;->error:Z
    invoke-static {v0, p1, v1}, Ldev/nino/squareglass/SquareOutput;->result(Ljava/lang/Object;Ljava/lang/Object;Z)V
    return-void
.end method
