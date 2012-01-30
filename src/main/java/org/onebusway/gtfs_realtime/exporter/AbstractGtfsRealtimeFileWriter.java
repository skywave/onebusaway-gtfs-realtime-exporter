package org.onebusway.gtfs_realtime.exporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public abstract class AbstractGtfsRealtimeFileWriter {

  private static final Logger _log = LoggerFactory.getLogger(AbstractGtfsRealtimeFileWriter.class);

  protected GtfsRealtimeProvider _provider;

  private ScheduledExecutorService _executor;

  private File _path;

  private int _period = 5;

  private ScheduledFuture<?> _task;

  @Inject
  public void setProvider(GtfsRealtimeProvider provider) {
    _provider = provider;
  }

  @Inject
  public void setExecutor(
      @Named(GtfsRealtimeExporterModule.NAME_EXECUTOR) ScheduledExecutorService executor) {
    _executor = executor;
  }

  public void setPath(File path) {
    _path = path;
  }

  public void setPeriod(int timeInSeconds) {
    _period = timeInSeconds;
  }

  @PostConstruct
  public void start() {
    _task = _executor.scheduleAtFixedRate(new TaskEntryPoint(), 0, _period,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    if (_task != null) {
      _task.cancel(false);
      _task = null;
    }
  }

  protected abstract Message getMessage();

  protected void writeMessageToFile() throws IOException {
    Message message = getMessage();
    OutputStream out = new BufferedOutputStream(new FileOutputStream(_path));
    message.writeTo(out);
    out.close();
  }

  private class TaskEntryPoint implements Runnable {

    @Override
    public void run() {
      try {
        writeMessageToFile();
      } catch (IOException ex) {
        _log.error("Error writing message to output file: " + _path, ex);
      }
    }
  }

}