.class public final Lradiant/LyricsReinitGuard;
.super Ljava/lang/Object;
.source "LyricsReinitGuard.smali"


# static fields

# stuff that makes lyrics view stays open.. i forgot what it actually does other than short circuit a call or some shit <3
.field public static lastMediaId:I

.field public static lastVm:Lcom/tidal/android/feature/playerscreen/ui/PlayerViewModel;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    const/4 v0, -0x1

    sput v0, Lradiant/LyricsReinitGuard;->lastMediaId:I

    const/4 v0, 0x0

    sput-object v0, Lradiant/LyricsReinitGuard;->lastVm:Lcom/tidal/android/feature/playerscreen/ui/PlayerViewModel;

    return-void
.end method
