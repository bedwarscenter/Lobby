package center.bedwars.lobby.configuration;

import center.bedwars.lobby.service.IService;

public interface IConfigurationService extends IService {
    long reloadConfigurations();

    long saveConfigurations();

    void reload();

    void save();
}
