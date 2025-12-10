package center.bedwars.lobby.dependency;

public interface IDependency {

    String getDependencyName();

    boolean isPresent();

    boolean isApiAvailable();
}
