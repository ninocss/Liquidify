.class public final Ldev/nino/squareglass/NativeArtworkBridge;
.super Ljava/lang/Object;
.method public static square(Landroid/view/View;)V
    .locals 1
    instance-of v0, p0, Landroidx/cardview/widget/CardView;
    if-eqz v0, :done
    check-cast p0, Landroidx/cardview/widget/CardView;
    const/4 v0, 0x0
    invoke-virtual {p0, v0}, Landroidx/cardview/widget/CardView;->setRadius(F)V
    invoke-virtual {p0, v0}, Landroidx/cardview/widget/CardView;->setCardElevation(F)V
    invoke-virtual {p0, v0}, Landroidx/cardview/widget/CardView;->setUseCompatPadding(Z)V
    :done
    return-void
.end method
.method public static continuousScroll(Landroid/view/View;)V
    .locals 2
    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;
    move-result-object v0
    instance-of v1, v0, Lcom/google/android/material/appbar/AppBarLayout$LayoutParams;
    if-eqz v1, :done
    check-cast v0, Lcom/google/android/material/appbar/AppBarLayout$LayoutParams;
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Lcom/google/android/material/appbar/AppBarLayout$LayoutParams;->setScrollFlags(I)V
    invoke-virtual {p0, v0}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V
    :done
    return-void
.end method
