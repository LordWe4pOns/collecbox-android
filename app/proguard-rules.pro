# Keep Room schemas and entities
-keep class corsica.universita.collecbox.data.model.** { *; }

# Keep Glance and WorkManager workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver {
    public <init>();
}

