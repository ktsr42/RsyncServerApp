Rsync Server 
------------

This app makes the shared storage of the device available via the Rsync protocol. Shared storage is what you can see in the "File Manager" app that usually comes with the device. This includes the 'DCIM' camera image folder as well as 'Downloads'.

When the "start" button is pressed and the device has Wifi connectivity, the app will open a rsync server on a random port with a random tag ("module" in rsync parlance). It will display all necessary details on how to connect to that server while it is running, e.g.

rsync://192.168.123.123:66671/abcdef/

Using a standard rsync client the server can then be interrogated and files moved back and forth, i.e. to and from the phone. The benefit is that the transfers can be driven from the client desktop, the keyboard and mouse interface of which making the process much more convenient.

Note: This app has not been widely tested and should therefore be considered alpha quality.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.ktsr42.rsyncserver/)
