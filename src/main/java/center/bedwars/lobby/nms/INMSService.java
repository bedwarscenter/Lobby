package center.bedwars.lobby.nms;

import center.bedwars.lobby.service.IService;

public interface INMSService extends IService {
    boolean isAvailable();

    String getVersion();
}
