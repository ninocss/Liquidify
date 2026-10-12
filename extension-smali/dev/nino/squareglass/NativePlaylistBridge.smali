.class public final Ldev/nino/squareglass/NativePlaylistBridge;
.super Ljava/lang/Object;

.method public static revision(Ljava/lang/Object;)Ljava/lang/String;
    .locals 4
    check-cast p0, Lcom/aspiro/wamp/model/Playlist;
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {p0}, Lcom/aspiro/wamp/model/Playlist;->getUuid()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0,v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v1, "|"
    invoke-virtual {v0,v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {p0}, Lcom/aspiro/wamp/model/Playlist;->getNumberOfItems()I
    move-result v2
    invoke-virtual {v0,v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method

.method public static contentRevision(Ljava/lang/Object;)Ljava/lang/String;
    .locals 3
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    iget-object p0,p0, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    new-instance v0,Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {p0}, Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;->getPlaylist()Lcom/aspiro/wamp/model/Playlist;
    move-result-object v1
    invoke-virtual {v1}, Lcom/aspiro/wamp/model/Playlist;->getUuid()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0,v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v1,"|"
    invoke-virtual {v0,v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {p0}, Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;->getPlaylistItems()Ljava/util/List;
    move-result-object v1
    invoke-interface {v1}, Ljava/util/List;->size()I
    move-result v1
    invoke-virtual {v0,v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method

.method public static metadata(Ljava/lang/Object;Ljava/lang/Object;)V
    .locals 11
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    check-cast p1, Lcom/aspiro/wamp/model/Playlist;
    iget-object v0, p0, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    move-object v1,p1
    const/4 v2,0x0
    const/4 v3,0x0
    const/4 v4,0x0
    const/4 v5,0x0
    const/4 v6,0x0
    const/4 v7,0x0
    const/16 v8,0x7e
    const/4 v9,0x0
    invoke-static/range {v0 .. v9}, Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;->copy$default(Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;Lcom/aspiro/wamp/model/Playlist;Ljava/util/List;ZZLjava/util/List;ZZILjava/lang/Object;)Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    move-result-object v10
    iput-object v10,p0, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    return-void
.end method

.method public static view(Ljava/lang/Object;)Landroid/view/View;
    .locals 0
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    iget-object p0, p0, Lcom/aspiro/wamp/playlist/ui/items/o;->v:Lcom/aspiro/wamp/playlist/ui/items/PlaylistItemCollectionView;
    return-object p0
.end method

.method public static items(Ljava/lang/Object;)Ljava/util/List;
    .locals 0
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    iget-object p0, p0, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    invoke-virtual {p0}, Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;->getItems()Ljava/util/List;
    move-result-object p0
    return-object p0
.end method

.method public static model(Ljava/lang/Object;Ljava/lang/Object;)V
    .locals 0
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    check-cast p1, Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    iput-object p1, p0, Lcom/aspiro/wamp/playlist/ui/items/o;->x:Lcom/aspiro/wamp/playlist/viewmodel/PlaylistCollectionViewModel;
    return-void
.end method

.method public static callback(Ljava/lang/Object;Ljava/util/List;)Ljava/lang/Object;
    .locals 1
    invoke-static {p0}, Ldev/nino/squareglass/NativePlaylistBridge;->items(Ljava/lang/Object;)Ljava/util/List;
    move-result-object p0
    new-instance v0, Lpj/b;
    invoke-direct {v0, p0, p1}, Lpj/b;-><init>(Ljava/util/List;Ljava/util/List;)V
    return-object v0
.end method

.method public static dispatch(Ljava/lang/Object;Ljava/lang/Object;)V
    .locals 0
    check-cast p0, Lcom/aspiro/wamp/playlist/ui/items/o;
    iget-object p0, p0, Lcom/aspiro/wamp/playlist/ui/items/o;->v:Lcom/aspiro/wamp/playlist/ui/items/PlaylistItemCollectionView;
    check-cast p1, Landroidx/recyclerview/widget/DiffUtil$DiffResult;
    invoke-virtual {p0,p1}, Lcom/aspiro/wamp/playlist/ui/items/PlaylistItemCollectionView;->b(Landroidx/recyclerview/widget/DiffUtil$DiffResult;)V
    return-void
.end method
