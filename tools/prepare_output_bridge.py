"""Generate pinned native output ABI helpers; no reflection or label matching."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'extension-smali/dev/nino/squareglass'
BRIDGE = 'Ldev/nino/squareglass/NativeOutputBridge;'
MENU = 'Ldev/nino/squareglass/SquareOutput;'
CONSUMER = 'Ldev/nino/squareglass/OutputConsumer;'
MANAGER = 'Lcom/aspiro/wamp/broadcast/h;'


def method(signature, locals, body):
    return f'\n.method public static {signature}\n    .locals {locals}\n\n{body}\n.end method\n'


def manager():
    return '''    invoke-static {}, Lcom/aspiro/wamp/broadcast/m;->a()Lcom/aspiro/wamp/broadcast/h;
    move-result-object v0'''


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    source = f'.class public final {BRIDGE}\n.super Ljava/lang/Object;\n'
    source += method('open()Ljava/lang/Object;', 2, manager() + '''
    new-instance v1, Ldev/nino/squareglass/OutputListener;
    invoke-direct {v1}, Ldev/nino/squareglass/OutputListener;-><init>()V
    invoke-interface {v0, v1}, Lcom/aspiro/wamp/broadcast/h;->addListener(Lcom/aspiro/wamp/broadcast/l;)V
    return-object v1''')
    source += method('scan()V', 1, manager() + f'\n    invoke-interface {{v0}}, {MANAGER}->startScanning()V\n    return-void')
    source += method('close(Ljava/lang/Object;)V', 1, manager() + f'''
    check-cast p0, Lcom/aspiro/wamp/broadcast/l;
    invoke-interface {{v0, p0}}, {MANAGER}->l(Lcom/aspiro/wamp/broadcast/l;)V
    invoke-interface {{v0}}, {MANAGER}->stopScanning()V
    return-void''')
    source += method('active()Ljava/lang/Object;', 1, manager() + f'\n    invoke-interface {{v0}}, {MANAGER}->e()Lp2/a;\n    move-result-object v0\n    invoke-static {{v0}}, Ldev/nino/squareglass/LinnProvider;->confirmed(Ljava/lang/Object;)Ljava/lang/Object;\n    move-result-object v0\n    return-object v0')
    source += method('select(Ljava/lang/Object;)V', 1, manager() + f'\n    invoke-static {{p0}}, Ldev/nino/squareglass/LinnProvider;->prepareSelection(Ljava/lang/Object;)V\n    check-cast p0, Lp2/a;\n    invoke-interface {{v0, p0}}, {MANAGER}->j(Lp2/a;)V\n    return-void')
    # Failed Linn selection must release v.c without calling the local provider's
    # connect(), which can reactivate local audio after an uncertain remote write.
    source += method('restoreLocalSelection(Ljava/lang/Object;)V', 3, manager() + '''
    instance-of v1, p0, Lp2/c;
    if-eqz v1, :done
    check-cast v0, Lcom/aspiro/wamp/broadcast/v;
    iget-object v1, v0, Lcom/aspiro/wamp/broadcast/v;->c:Lp2/a;
    instance-of v1, v1, Ldev/nino/squareglass/LinnProvider$Device;
    if-eqz v1, :done
    check-cast p0, Lp2/a;
    iput-object p0, v0, Lcom/aspiro/wamp/broadcast/v;->c:Lp2/a;
    invoke-virtual {v0, p0}, Lcom/aspiro/wamp/broadcast/v;->m(Lp2/a;)Lcom/aspiro/wamp/broadcast/o;
    move-result-object v1
    instance-of v2, v1, Lcom/aspiro/wamp/broadcast/w;
    if-eqz v2, :notify
    check-cast v1, Lcom/aspiro/wamp/broadcast/w;
    const/4 v2, 0x1
    iput-boolean v2, v1, Lcom/aspiro/wamp/broadcast/w;->d:Z
    :notify
    invoke-virtual {v0}, Lcom/aspiro/wamp/broadcast/v;->c()V
    :done
    return-void''')
    for name, field in [('id', 'a'), ('name', 'b')]:
        source += method(name + '(Ljava/lang/Object;)Ljava/lang/String;', 1,
                         f'    check-cast p0, Lp2/a;\n    iget-object v0, p0, Lp2/a;->{field}:Ljava/lang/String;\n    return-object v0')
    source += method('cancel(Ljava/lang/Object;)V', 0, '''    if-eqz p0, :done
    check-cast p0, Lio/reactivex/disposables/Disposable;
    invoke-interface {p0}, Lio/reactivex/disposables/Disposable;->dispose()V
    :done
    return-void''')
    source += method('type(Ljava/lang/Object;)I', 1, '    check-cast p0, Lp2/a;\n    iget v0, p0, Lp2/a;->c:I\n    return v0')
    source += method('local(Ljava/lang/Object;)Z', 1, '    instance-of v0, p0, Lp2/c;\n    return v0')
    source += method('discard(Ljava/lang/Object;)V', 1, '''    check-cast p0, Lcom/aspiro/wamp/broadcast/f;
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->d:Lio/reactivex/disposables/Disposable;
    invoke-static {v0}, Ldev/nino/squareglass/NativeOutputBridge;->cancel(Ljava/lang/Object;)V
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->e:Lfq/f;
    if-eqz v0, :volume_done
    invoke-interface {v0}, Lfq/f;->stopListening()V
    :volume_done
    iget-object v0, p0, Lcom/aspiro/wamp/broadcast/f;->c:Lrx/subscriptions/CompositeSubscription;
    invoke-virtual {v0}, Lrx/subscriptions/CompositeSubscription;->unsubscribe()V
    return-void''')
    source += method('request(Ljava/lang/Object;)Ljava/lang/Object;', 4, manager() + f'''
    invoke-interface {{v0}}, {MANAGER}->i()Lio/reactivex/Single;
    move-result-object v0
    invoke-static {{}}, Lio/reactivex/android/schedulers/AndroidSchedulers;->mainThread()Lio/reactivex/Scheduler;
    move-result-object v1
    invoke-virtual {{v0, v1}}, Lio/reactivex/Single;->observeOn(Lio/reactivex/Scheduler;)Lio/reactivex/Single;
    move-result-object v0
    const/4 v3, 0x0
    new-instance v1, {CONSUMER}
    invoke-direct {{v1, p0, v3}}, {CONSUMER}-><init>(Ljava/lang/Object;Z)V
    const/4 v3, 0x1
    new-instance v2, {CONSUMER}
    invoke-direct {{v2, p0, v3}}, {CONSUMER}-><init>(Ljava/lang/Object;Z)V
    invoke-virtual {{v0, v1, v2}}, Lio/reactivex/Single;->subscribe(Lio/reactivex/functions/Consumer;Lio/reactivex/functions/Consumer;)Lio/reactivex/disposables/Disposable;
    move-result-object v0
    return-object v0''')
    (DEST / 'NativeOutputBridge.smali').write_text(source, encoding='utf-8', newline='\n')
    consumer = f'''.class public final {CONSUMER}
.super Ljava/lang/Object;
.implements Lio/reactivex/functions/Consumer;
.field private final callback:Ljava/lang/Object;
.field private final error:Z
.method public constructor <init>(Ljava/lang/Object;Z)V
    .locals 0
    invoke-direct {{p0}}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, {CONSUMER}->callback:Ljava/lang/Object;
    iput-boolean p2, p0, {CONSUMER}->error:Z
    return-void
.end method
.method public accept(Ljava/lang/Object;)V
    .locals 2
    iget-object v0, p0, {CONSUMER}->callback:Ljava/lang/Object;
    iget-boolean v1, p0, {CONSUMER}->error:Z
    invoke-static {{v0, p1, v1}}, {MENU}->result(Ljava/lang/Object;Ljava/lang/Object;Z)V
    return-void
.end method
'''
    (DEST / 'OutputConsumer.smali').write_text(consumer, encoding='utf-8', newline='\n')
    listener = '''.class public final Ldev/nino/squareglass/OutputListener;
.super Ljava/lang/Object;
.implements Lcom/aspiro/wamp/broadcast/l;
.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method
'''
    for signature in ('a(Lcom/aspiro/wamp/broadcast/o;)V', 'b(Lcom/aspiro/wamp/broadcast/o;I)V',
                      'c()V', 'd(Lcom/aspiro/wamp/broadcast/o;)V'):
        listener += f'''
.method public {signature}
    .locals 0
    invoke-static {{}}, {MENU}->changed()V
    return-void
.end method
'''
    (DEST / 'OutputListener.smali').write_text(listener, encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
