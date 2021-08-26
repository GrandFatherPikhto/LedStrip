# Приложение для управления адресной лентой светодиодов с мобильного телефона при помощи ESP32
## Описание
Это приложение предназначено для управление через нешифрованный Bluetooth Low Energy устройством ESP32. Приложение управляет цветом и яркостью светодиодов, может включать различные режимы работы устройства.  
## Добавить в AndroidManifest.xml
Разрешения для работы с Bluetooth.
```xml
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
```
Для того, чтобы сервис мог запуститься, необходимо добавить описание сервиса:
```xml
<service android:name=".service.BluetoothLeService" android:enabled="true" />
```
