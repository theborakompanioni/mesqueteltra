package org.tbk.mesqueteltra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.boot.system.EmbeddedServerPortFileWriter;
import org.springframework.context.ApplicationListener;

@Slf4j
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(Application.class)
                .listeners(applicationPidFileWriter(), embeddedServerPortFileWriter())
                .web(true)
                .run(args);
    }

    static ApplicationListener<?> applicationPidFileWriter() {
        return new ApplicationPidFileWriter("app.pid");
    }

    static ApplicationListener<?> embeddedServerPortFileWriter() {
        return new EmbeddedServerPortFileWriter("app.port");
    }
}
