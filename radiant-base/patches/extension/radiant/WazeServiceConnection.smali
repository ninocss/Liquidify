.class public final Lradiant/WazeServiceConnection;
.super Ljava/lang/Object;
.source "WazeServiceConnection.smali"

# interfaces
.implements Landroid/content/ServiceConnection;


# instance fields
.field private final context:Landroid/content/Context;

.field private final token:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/content/Context;Ljava/lang/String;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lradiant/WazeServiceConnection;->context:Landroid/content/Context;

    iput-object p2, p0, Lradiant/WazeServiceConnection;->token:Ljava/lang/String;

    return-void
.end method

.method private addOpenMeIntent(Landroid/os/Bundle;)V
    .locals 4

    iget-object v0, p0, Lradiant/WazeServiceConnection;->context:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v1

    invoke-virtual {v0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Landroid/content/pm/PackageManager;->getLaunchIntentForPackage(Ljava/lang/String;)Landroid/content/Intent;

    move-result-object v1

    if-eqz v1, :return

    const/high16 v2, 0x10000000

    invoke-virtual {v1, v2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    const v2, 0xc000000

    const/4 v3, 0x0

    invoke-static {v0, v3, v1, v2}, Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;

    move-result-object v1

    const-string v2, "openMeIntent"

    invoke-virtual {p1, v2, v1}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    :return
    return-void
.end method

.method private createClientMessenger()Landroid/os/Messenger;
    .locals 3

    new-instance v0, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v1

    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    new-instance v2, Landroid/os/Messenger;

    invoke-direct {v2, v0}, Landroid/os/Messenger;-><init>(Landroid/os/Handler;)V

    return-object v2
.end method

.method private createConfigBundle()Landroid/os/Bundle;
    .locals 3

    new-instance v0, Landroid/os/Bundle;

    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V

    const-string v1, "versionCode"

    const/16 v2, 0xa

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    const-string v1, "themeColor"

    const v2, __RL_WAZE_THEME_COLOR__ # Waze SDK themeColor

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    invoke-direct {p0, v0}, Lradiant/WazeServiceConnection;->addOpenMeIntent(Landroid/os/Bundle;)V

    return-object v0
.end method

.method private register(Landroid/os/IBinder;)V
    .locals 5

    if-eqz p1, :return

    invoke-static {}, Landroid/os/Parcel;->obtain()Landroid/os/Parcel;

    move-result-object v0

    invoke-static {}, Landroid/os/Parcel;->obtain()Landroid/os/Parcel;

    move-result-object v1

    const-string v2, "com.waze.sdk.ISdkService"

    invoke-virtual {v0, v2}, Landroid/os/Parcel;->writeInterfaceToken(Ljava/lang/String;)V

    iget-object v2, p0, Lradiant/WazeServiceConnection;->token:Ljava/lang/String;

    invoke-virtual {v0, v2}, Landroid/os/Parcel;->writeString(Ljava/lang/String;)V

    invoke-direct {p0}, Lradiant/WazeServiceConnection;->createConfigBundle()Landroid/os/Bundle;

    move-result-object v2

    invoke-static {v0, v2}, Lradiant/WazeServiceConnection;->writeParcelable(Landroid/os/Parcel;Landroid/os/Parcelable;)V

    invoke-direct {p0}, Lradiant/WazeServiceConnection;->createClientMessenger()Landroid/os/Messenger;

    move-result-object v2

    invoke-static {v0, v2}, Lradiant/WazeServiceConnection;->writeParcelable(Landroid/os/Parcel;Landroid/os/Parcelable;)V

    const/4 v2, 0x1

    const/4 v3, 0x0

    invoke-interface {p1, v2, v0, v1, v3}, Landroid/os/IBinder;->transact(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z

    move-result v4

    if-eqz v4, :cleanup

    invoke-virtual {v1}, Landroid/os/Parcel;->readException()V

    :cleanup
    invoke-virtual {v1}, Landroid/os/Parcel;->recycle()V

    invoke-virtual {v0}, Landroid/os/Parcel;->recycle()V

    :return
    return-void
.end method

.method private safeUnbind()V
    .locals 1

    :try_start_0
    iget-object v0, p0, Lradiant/WazeServiceConnection;->context:Landroid/content/Context;

    invoke-virtual {v0, p0}, Landroid/content/Context;->unbindService(Landroid/content/ServiceConnection;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    goto :return

    :catch_0
    move-exception v0

    :return
    return-void
.end method

.method private static writeParcelable(Landroid/os/Parcel;Landroid/os/Parcelable;)V
    .locals 2

    const/4 v0, 0x0

    if-eqz p1, :null_value

    const/4 v1, 0x1

    invoke-virtual {p0, v1}, Landroid/os/Parcel;->writeInt(I)V

    invoke-interface {p1, p0, v0}, Landroid/os/Parcelable;->writeToParcel(Landroid/os/Parcel;I)V

    return-void

    :null_value
    invoke-virtual {p0, v0}, Landroid/os/Parcel;->writeInt(I)V

    return-void
.end method


# virtual methods
.method public onServiceConnected(Landroid/content/ComponentName;Landroid/os/IBinder;)V
    .locals 1

    :try_start_0
    invoke-direct {p0, p2}, Lradiant/WazeServiceConnection;->register(Landroid/os/IBinder;)V
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    goto :unbind

    :catch_0
    move-exception v0

    :unbind
    invoke-direct {p0}, Lradiant/WazeServiceConnection;->safeUnbind()V

    return-void
.end method

.method public onServiceDisconnected(Landroid/content/ComponentName;)V
    .locals 0

    return-void
.end method
