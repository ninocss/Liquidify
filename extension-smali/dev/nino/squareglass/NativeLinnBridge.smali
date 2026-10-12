.class public final Ldev/nino/squareglass/NativeLinnBridge;
.super Ljava/lang/Object;
.method public static player()Lcom/aspiro/wamp/player/k0;
    .locals 1
    invoke-static {}, Lcom/aspiro/wamp/player/MusicService;->c()Lcom/aspiro/wamp/player/k0;
    move-result-object v0
    return-object v0
.end method
.method public static activate(Lcom/aspiro/wamp/player/k0;)V
    .locals 5
    invoke-static {}, Lcom/aspiro/wamp/player/d$c;->a()Lcom/aspiro/wamp/player/d;
    move-result-object v0
    iget-object v1, v0, Lcom/aspiro/wamp/player/d;->l:Lcom/aspiro/wamp/player/k0;
    invoke-interface {v1}, Lcom/aspiro/wamp/player/k0;->getCurrentMediaPosition()I
    move-result v2
    invoke-interface {v1}, Lcom/aspiro/wamp/player/k0;->onDeactivated()V
    iget-object v3, v0, Lcom/aspiro/wamp/player/d;->e:Lcom/aspiro/wamp/player/PlaybackLifecycleHolder;
    sget-object v4, Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;->INACTIVE:Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;
    iput-object v4, v3, Lcom/aspiro/wamp/player/PlaybackLifecycleHolder;->a:Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;
    iget-object v4, v0, Lcom/aspiro/wamp/player/d;->c:Lcom/aspiro/wamp/player/s0;
    invoke-virtual {v4}, Lcom/aspiro/wamp/player/s0;->e()V
    invoke-interface {p0, v2, v1}, Lcom/aspiro/wamp/player/k0;->onActivated(ILcom/aspiro/wamp/player/k0;)V
    sget-object v4, Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;->ACTIVE:Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;
    iput-object v4, v3, Lcom/aspiro/wamp/player/PlaybackLifecycleHolder;->a:Lcom/aspiro/wamp/player/PlaybackLifecycleHolder$State;
    iput-object p0, v0, Lcom/aspiro/wamp/player/d;->l:Lcom/aspiro/wamp/player/k0;
    return-void
.end method
.method public static publish(Lcom/aspiro/wamp/enums/MusicServiceState;II)V
    .locals 2
    invoke-static {}, Lcom/aspiro/wamp/player/d$c;->a()Lcom/aspiro/wamp/player/d;
    move-result-object v0
    iget-object v1, v0, Lcom/aspiro/wamp/player/d;->a:Lcom/aspiro/wamp/player/o0;
    invoke-virtual {v1, p0}, Lcom/aspiro/wamp/player/o0;->a(Lcom/aspiro/wamp/enums/MusicServiceState;)V
    iget-object v1, v0, Lcom/aspiro/wamp/player/d;->c:Lcom/aspiro/wamp/player/s0;
    invoke-virtual {v1, p1, p2}, Lcom/aspiro/wamp/player/s0;->c(II)V
    invoke-virtual {v1, p1, p2}, Lcom/aspiro/wamp/player/s0;->d(II)V
    return-void
.end method
.method public static disconnected(Ljava/lang/Object;)V
    .locals 0
    if-eqz p0, :done
    check-cast p0, Lcom/aspiro/wamp/broadcast/t;
    invoke-virtual {p0}, Lcom/aspiro/wamp/broadcast/t;->a()V
    :done
    return-void
.end method
.method public static restoreShuffle(Lcom/aspiro/wamp/playqueue/y;Z)V
    .locals 1
    instance-of v0, p0, Lcom/aspiro/wamp/playqueue/s;
    if-eqz v0, :done
    check-cast p0, Lcom/aspiro/wamp/playqueue/s;
    iget-object v0, p0, Lcom/aspiro/wamp/playqueue/s;->g:Lcom/aspiro/wamp/playqueue/b1;
    iput-boolean p1, v0, Lcom/aspiro/wamp/playqueue/b1;->e:Z
    :done
    return-void
.end method
.method public static skip(Lcom/aspiro/wamp/playqueue/y;Z)I
    .locals 3
    instance-of v0, p0, Lcom/aspiro/wamp/playqueue/s;
    if-eqz v0, :missing
    move-object v0, p0
    check-cast v0, Lcom/aspiro/wamp/playqueue/s;
    if-eqz p1, :next
    iget-object v1, v0, Lcom/aspiro/wamp/playqueue/s;->g:Lcom/aspiro/wamp/playqueue/b1;
    invoke-virtual {v1}, Lcom/aspiro/wamp/playqueue/b1;->n()Lcom/aspiro/wamp/playqueue/u0;
    move-result-object v2
    iget-object v1, v0, Lcom/aspiro/wamp/playqueue/s;->a:Lcom/aspiro/wamp/playqueue/z;
    invoke-interface {v1}, Lcom/aspiro/wamp/playqueue/z;->c()V
    invoke-interface {p0}, Lcom/aspiro/wamp/playqueue/y;->getRepeatMode()Lcom/aspiro/wamp/enums/RepeatMode;
    move-result-object v1
    invoke-virtual {v0, v1}, Lcom/aspiro/wamp/playqueue/s;->d(Lcom/aspiro/wamp/enums/RepeatMode;)V
    goto :selected
    :next
    invoke-virtual {v0}, Lcom/aspiro/wamp/playqueue/s;->a()Lcom/aspiro/wamp/playqueue/u0;
    move-result-object v2
    :selected
    if-eqz v2, :missing
    invoke-interface {p0}, Lcom/aspiro/wamp/playqueue/y;->getCurrentItemPosition()I
    move-result v0
    return v0
    :missing
    const/4 v0, -0x1
    return v0
.end method
