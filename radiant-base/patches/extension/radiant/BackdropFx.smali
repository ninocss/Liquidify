.class public final Lradiant/BackdropFx;
.super Ljava/lang/Object;


.method public static apply(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;
    .locals 14

    const v0, __RL_CONTRAST__

    const v1, __RL_SATURATION__

    const v2, __RL_BRIGHTNESS__

    # cm = ColorMatrix(); cm.setSaturation(s)
    new-instance v3, Landroid/graphics/ColorMatrix;

    invoke-direct {v3}, Landroid/graphics/ColorMatrix;-><init>()V

    invoke-virtual {v3, v1}, Landroid/graphics/ColorMatrix;->setSaturation(F)V

    # o = 128 - 128*c  (contrast pivot at mid-grey, 0..255 space)
    const/high16 v4, 0x43000000

    mul-float v5, v4, v0

    sub-float v5, v4, v5

    # contrast matrix: diag = c, offset column = o
    const/16 v6, 0x14

    new-array v7, v6, [F

    const/4 v8, 0x0

    aput v0, v7, v8

    const/4 v8, 0x4

    aput v5, v7, v8

    const/4 v8, 0x6

    aput v0, v7, v8

    const/16 v8, 0x9

    aput v5, v7, v8

    const/16 v8, 0xc

    aput v0, v7, v8

    const/16 v8, 0xe

    aput v5, v7, v8

    const/high16 v9, 0x3f800000

    const/16 v8, 0x12

    aput v9, v7, v8

    new-instance v8, Landroid/graphics/ColorMatrix;

    invoke-direct {v8, v7}, Landroid/graphics/ColorMatrix;-><init>([F)V

    invoke-virtual {v3, v8}, Landroid/graphics/ColorMatrix;->postConcat(Landroid/graphics/ColorMatrix;)V

    # brightness matrix: diag = b
    new-array v7, v6, [F

    const/4 v8, 0x0

    aput v2, v7, v8

    const/4 v8, 0x6

    aput v2, v7, v8

    const/16 v8, 0xc

    aput v2, v7, v8

    const/16 v8, 0x12

    aput v9, v7, v8

    new-instance v8, Landroid/graphics/ColorMatrix;

    invoke-direct {v8, v7}, Landroid/graphics/ColorMatrix;-><init>([F)V

    invoke-virtual {v3, v8}, Landroid/graphics/ColorMatrix;->postConcat(Landroid/graphics/ColorMatrix;)V

    # out = createBitmap(w, h, ARGB_8888)
    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v10

    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v11

    sget-object v12, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {v10, v11, v12}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v10

    # canvas over out
    new-instance v11, Landroid/graphics/Canvas;

    invoke-direct {v11, v10}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    # paint with the color-matrix filter
    new-instance v12, Landroid/graphics/Paint;

    invoke-direct {v12}, Landroid/graphics/Paint;-><init>()V

    new-instance v13, Landroid/graphics/ColorMatrixColorFilter;

    invoke-direct {v13, v3}, Landroid/graphics/ColorMatrixColorFilter;-><init>(Landroid/graphics/ColorMatrix;)V

    invoke-virtual {v12, v13}, Landroid/graphics/Paint;->setColorFilter(Landroid/graphics/ColorFilter;)Landroid/graphics/ColorFilter;

    # draw the blurred bitmap through the filter
    const/4 v0, 0x0

    invoke-virtual {v11, p0, v0, v0, v12}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V

    invoke-virtual {p0}, Landroid/graphics/Bitmap;->recycle()V

    return-object v10
.end method
