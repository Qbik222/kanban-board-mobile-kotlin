# If minify is enabled, keep socket.io client (see also backend socket.io version alignment).
-keep class io.socket.** { *; }
-dontwarn io.socket.**
