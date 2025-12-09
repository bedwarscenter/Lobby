package center.bedwars.lobby.injection;

import center.bedwars.lobby.service.IService;
import com.google.inject.Injector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ServiceManager {

    private final Injector injector;
    private final Logger logger;
    private final List<IService> enabledServices = new ArrayList<>();

    public ServiceManager(Injector injector, Logger logger) {
        this.injector = injector;
        this.logger = logger;
    }

    public <T extends IService> T enable(Class<T> serviceClass) {
        T service = injector.getInstance(serviceClass);
        service.enable();
        enabledServices.add(service);
        logger.info("Enabled service: " + serviceClass.getSimpleName());
        return service;
    }

    public void disableAll() {
        for (int i = enabledServices.size() - 1; i >= 0; i--) {
            try {
                IService service = enabledServices.get(i);
                service.disable();
                logger.info("Disabled service: " + service.getClass().getSimpleName());
            } catch (Exception e) {
                logger.severe("Failed to disable service: " + enabledServices.get(i).getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        enabledServices.clear();
    }

    public <T> T get(Class<T> serviceClass) {
        return injector.getInstance(serviceClass);
    }
}
