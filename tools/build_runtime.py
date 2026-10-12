"""Runtime paths shared by local and GitHub Actions patch builds."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_HOME = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Android\Android Studio\jbr'))
SDK_HOME = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'AppData/Local/Android/Sdk')))


def java_tool(name):
    return JAVA_HOME / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))


def sdk_tool(directory, name):
    return directory / (name + ('.bat' if os.name == 'nt' else ''))


def gradle_wrapper():
    return ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
