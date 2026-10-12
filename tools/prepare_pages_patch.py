"""Exact recommendation transaction hooks; preserve native Add and DiffUtil identity."""
from prepare_visual_patch import save


def method(text, signature, change):
    start=text.index(signature);end=text.index('.end method',start)
    return text[:start]+change(text[start:end])+text[end:]


def presenter(text):
    def begin(body):
        marker='    :cond_1\n'
        assert body.count(marker)==1
        return body.replace(marker,marker+'    invoke-static {p0}, Ldev/nino/squareglass/RecommendationUpdates;->begin(Ljava/lang/Object;)V\n\n')
    text=method(text,'.method public final h(',begin)
    def update(body):
        marker='    .locals 3\n'
        assert body.count(marker)==1
        return body.replace(marker,marker+'''
    invoke-static {p0, p1}, Ldev/nino/squareglass/RecommendationUpdates;->handle(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :square_native_diff
    return-void
    :square_native_diff
''')
    text=method(text,'.method public final o(',update)
    def reload(body):
        marker='    .locals 12\n'
        assert body.count(marker)==1
        return body.replace(marker,marker+'''
    invoke-static {p0, p1}, Ldev/nino/squareglass/RecommendationUpdates;->reuse(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :square_native_reload
    return-void
    :square_native_reload
''')
    text=method(text,'.method public final l(',reload)
    def suggestions(body):
        marker='    .locals 4\n'
        assert body.count(marker)==1
        return body.replace(marker,marker+'''
    invoke-static {p0}, Ldev/nino/squareglass/RecommendationUpdates;->active(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :square_native_suggestions
    return-void
    :square_native_suggestions
''')
    text=method(text,'.method public final n(',suggestions)
    return method(text,'.method public final g(',lambda body:body.replace('    .locals 11\n','    .locals 11\n    invoke-static {p0}, Ldev/nino/squareglass/RecommendationUpdates;->manual(Ljava/lang/Object;)V\n'))


def result(text):
    def success(body):
        marker='    if-eqz v1, :cond_4'
        assert body.count(marker)==1
        body=body.replace('    invoke-virtual {v2, v3}, Lcom/aspiro/wamp/playlist/ui/items/o;->n(Z)V','    invoke-static {v2}, Ldev/nino/squareglass/RecommendationUpdates;->active(Ljava/lang/Object;)Z\n    move-result v1\n    if-nez v1, :cond_3\n    invoke-virtual {v2, v3}, Lcom/aspiro/wamp/playlist/ui/items/o;->n(Z)V')
        return body.replace(marker,'    invoke-static {v2, v1}, Ldev/nino/squareglass/RecommendationUpdates;->completed(Ljava/lang/Object;Z)V\n\n'+marker)
    text=method(text,'.method public final onNext(',success)
    def failure(body):
        body=body.replace('    .locals 0','    .locals 1')
        marker='    invoke-super {p0, p1}, Lr1/a;->onError(Ljava/lang/Throwable;)V'
        assert body.count(marker)==1
        return body.replace(marker,marker+'''
    iget-object p1, p0, Lcom/aspiro/wamp/playlist/ui/items/o$c;->b:Lcom/aspiro/wamp/playlist/ui/items/o;
    const/4 v0, 0x0
    invoke-static {p1, v0}, Ldev/nino/squareglass/RecommendationUpdates;->completed(Ljava/lang/Object;Z)V
''')
    return method(text,'.method public final onError(',failure)


def event(text):
    def append(body):
        marker='    iget-object v0, v1, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;'
        assert body.count(marker)==1
        body=body.replace(marker,'    invoke-static {v1, p1}, Ldev/nino/squareglass/RecommendationUpdates;->added(Ljava/lang/Object;Ljava/lang/Object;)V\n\n'+marker)
        marker='    if-le v0, v2, :cond_0'
        assert body.count(marker)==1
        return body.replace(marker,'''    invoke-static {v1}, Ldev/nino/squareglass/RecommendationUpdates;->active(Ljava/lang/Object;)Z
    move-result v2
    if-nez v2, :square_incremental_append
    const/4 v2, 0x1
    if-le v0, v2, :cond_0
    :square_incremental_append''')
    text=method(text,'.method public final a(',append)
    def change(body):
        marker='    invoke-virtual {v4, v2}, Lcom/aspiro/wamp/playlist/ui/items/o;->o(Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;)V'
        assert body.count(marker)==1
        # After the matching UUID update, retain normal event behavior outside a local Add.
        return body.replace(marker,marker+'''
    invoke-static {v4}, Ldev/nino/squareglass/RecommendationUpdates;->preserve(Ljava/lang/Object;)Z
    move-result v2
    if-eqz v2, :square_native_scroll
    return-void
    :square_native_scroll
''')
    return method(text,'.method public final k(',change)


if __name__=='__main__':
    base='com/aspiro/wamp/playlist/ui/items/'
    save(base+'o.smali','smali_classes21','147-square-recommendation-batch.patch',presenter)
    save(base+'o$c.smali','smali_classes21','148-square-recommendation-result.patch',result)
    save(base+'o$a.smali','smali_classes21','149-square-recommendation-position.patch',event)
    print('Prepared native recommendation Add transaction, coalesced diff and retained anchor')
