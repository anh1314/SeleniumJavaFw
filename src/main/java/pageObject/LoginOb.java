package pageObject;

import actions.common.BasePage;
import actions.common.ConfigLoader;

public class LoginOb extends BasePage {

public void openToEcoCardUrl(){
    openPageUrl(ConfigLoader.get("URL_ECO_CARD_PORTAL"));
}
}
