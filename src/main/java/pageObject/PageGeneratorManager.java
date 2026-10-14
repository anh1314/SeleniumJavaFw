package pageObject;

public class PageGeneratorManager {

    @SuppressWarnings("unchecked")
    public static <T> T getPage(Class<T> pageClass) {
        try {
            return pageClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + pageClass.getSimpleName(), e);
        }
    }
}
