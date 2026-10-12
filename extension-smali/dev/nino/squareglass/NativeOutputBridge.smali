.class public final Ldev/nino/squareglass/NativeOutputBridge;
.super Ljava/lang/Object;

.method public static open()Ljava/lang/Object;
    .locals 2

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    new-instance v1, Ldev/nino/squareglass/OutputListener;
    invoke-direct {v1}, Ldev/nino/squareglass/OutputListener;-><init>()V
    invoke-interface {v0, v1}, Lcom/aspiro/wamp/broadcast/h;->addListener(Lcom/aspiro/wamp/broadcast/l;)V
    return-object v1
.end method

.method public static scan()V
    .locals 1

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    invoke-interface {v0}, Lcom/aspiro/wamp/broadcast/h;->startScanning()V
    return-void
.end method

.method public static close(Ljava/lang/Object;)V
    .locals 1

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    check-cast p0, Lcom/aspiro/wamp/broadcast/l;
    invoke-interface {v0, p0}, Lcom/aspiro/wamp/broadcast/h;->l(Lcom/aspiro/wamp/broadcast/l;)V
    invoke-interface {v0}, Lcom/aspiro/wamp/broadcast/h;->stopScanning()V
    return-void
.end method

.method public static active()Ljava/lang/Object;
    .locals 1

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    invoke-interface {v0}, Lcom/aspiro/wamp/broadcast/h;->e()Lp2/a;
    move-result-object v0
    invoke-static {v0}, Ldev/nino/squareglass/LinnProvider;->confirmed(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    return-object v0
.end method

.method public static select(Ljava/lang/Object;)V
    .locals 1

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    invoke-static {p0}, Ldev/nino/squareglass/LinnProvider;->prepareSelection(Ljava/lang/Object;)V
    check-cast p0, Lp2/a;
    invoke-interface {v0, p0}, Lcom/aspiro/wamp/broadcast/h;->j(Lp2/a;)V
    return-void
.end method

.method public static id(Ljava/lang/Object;)Ljava/lang/String;
    .locals 1

    check-cast p0, Lp2/a;
    iget-object v0, p0, Lp2/a;->a:Ljava/lang/String;
    return-object v0
.end method

.method public static name(Ljava/lang/Object;)Ljava/lang/String;
    .locals 1

    check-cast p0, Lp2/a;
    iget-object v0, p0, Lp2/a;->b:Ljava/lang/String;
    return-object v0
.end method

.method public static cancel(Ljava/lang/Object;)V
    .locals 0

    if-eqz p0, :done
    check-cast p0, Lio/reactivex/disposables/Disposable;
    invoke-interface {p0}, Lio/reactivex/disposables/Disposable;->dispose()V
    :done
    return-void
.end method

.method public static type(Ljava/lang/Object;)I
    .locals 1

    check-cast p0, Lp2/a;
    iget v0, p0, Lp2/a;->c:I
    return v0
.end method

.method public static local(Ljava/lang/Object;)Z
    .locals 1

    instance-of v0, p0, Lp2/c;
    return v0
.end method

.method public static discard(Ljava/lang/Object;)V
    .locals 1

    check-cast p0, Lcom/aspiro/wamp/broadcast/f;
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->d:Lio/reactivex/disposables/Disposable;
    invoke-static {v0}, Ldev/nino/squareglass/NativeOutputBridge;->cancel(Ljava/lang/Object;)V
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->e:Lfq/f;
    if-eqz v0, :volume_done
    invoke-interface {v0}, Lfq/f;->stopListening()V
    :volume_done
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->c:Lrx/subscriptions/CompositeSubscription;
    invoke-virtual {v0}, Lrx/subscriptions/CompositeSubscription;->unsubscribe()V
    return-void
.end method

.method public static request(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 4

    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0
    invoke-interface {v0}, Lcom/aspiro/wamp/broadcast/h;->i()Lio/reactivex/Single;
    move-result-object v0
    invoke-static {}, Lio/reactivex/android/schedulers/AndroidSchedulers;->mainThread()Lio/reactivex/Scheduler;
    move-result-object v1
    invoke-virtual {v0, v1}, Lio/reactivex/Single;->observeOn(Lio/reactivex/Scheduler;)Lio/reactivex/Single;
    move-result-object v0
    const/4 v3, 0x0
    new-instance v1, Ldev/nino/squareglass/OutputConsumer;
    invoke-direct {v1, p0, v3}, Ldev/nino/squareglass/OutputConsumer;-><init>(Ljava/lang/Object;Z)V
    const/4 v3, 0x1
    new-instance v2, Ldev/nino/squareglass/OutputConsumer;
    invoke-direct {v2, p0, v3}, Ldev/nino/squareglass/OutputConsumer;-><init>(Ljava/lang/Object;Z)V
    invoke-virtual {v0, v1, v2}, Lio/reactivex/Single;->subscribe(Lio/reactivex/functions/Consumer;Lio/reactivex/functions/Consumer;)Lio/reactivex/disposables/Disposable;
    move-result-object v0
    return-object v0
.end method
