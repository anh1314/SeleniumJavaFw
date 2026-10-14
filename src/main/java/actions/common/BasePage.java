package actions.common;

import actions.listener.VerificationFailures;
import actions.helpers.SQLiteHelper;
import actions.helpers.JsonHelper;
import actions.utils.LogUtils;
import actions.drivers.DriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


public class BasePage {

    public void retryAction(Runnable action, String actionName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                lastException = e;
                logInfo("⚠️ Action {} failed - retrying attempt {} ....", actionName, attempt);
                sleepInSecond(1);
            }
        }

        logError("Action {} failed after 3 retries: {}", actionName, lastException);
        throw new RuntimeException("Action failed: " + actionName, lastException
        );
    }

    public <T> T retryAction(Supplier<T> action, String actionName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                logInfo("⚠️ Action {} failed at attempt {}", actionName, attempt);
                sleepInSecond(1);
            }
        }
        throw new RuntimeException("Action failed: " + actionName, lastException);
    }

    public void openPageUrl(String url) {
        DriverManager.getDriver().get(url);
        waitForPageLoaded();
    }

    public String getPageTitle() {
        return DriverManager.getDriver().getTitle();
    }

    public String getPageUrl() {
        return DriverManager.getDriver().getCurrentUrl();
    }

    public String getPageSource() {
        return DriverManager.getDriver().getPageSource();
    }

    public void backToPage() {
        DriverManager.getDriver().navigate().back();
    }

    public void forwardToPage() {
        DriverManager.getDriver().navigate().forward();
    }

    public void refreshPage() {
        DriverManager.getDriver().navigate().refresh();
    }

    public Alert waitForAlertPresence() {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        return explicitWait.until(ExpectedConditions.alertIsPresent());
    }

    public void acceptAlert() {
        waitForAlertPresence().accept();
    }

    public void cancelAlert() {
        waitForAlertPresence().dismiss();
    }

    public void getAlertText() {
        waitForAlertPresence().getText();
    }

    public void sendKeysToAlert(String textSenkey) {
        waitForAlertPresence().sendKeys(textSenkey);
    }

    public boolean isElementDisplay(String locator) {
        waitForElementVisible(locator);
        return getElement(locator).isDisplayed();
    }

    public void switchToWindowByID(String parentID) {
        Set<String> allWindows = DriverManager.getDriver().getWindowHandles();
        for (String runWindow : allWindows) {
            if (!runWindow.equals(parentID)) {
                DriverManager.getDriver().switchTo().window(runWindow);
                break;
            }
        }
    }

    public void switchToWindowByTitle(String title) {
        Set<String> allWindows = DriverManager.getDriver().getWindowHandles();
        for (String runWindows : allWindows) {
            DriverManager.getDriver().switchTo().window(runWindows);
            String currentWin = DriverManager.getDriver().getTitle();
            if (currentWin.equals(title)) {
                break;
            }
        }
    }

    public void closeAllWindowsWithoutParent(String parentID) {
        Set<String> allWindows = DriverManager.getDriver().getWindowHandles();
        for (String runWindows : allWindows) {
            if (!runWindows.equals(parentID)) {
                DriverManager.getDriver().switchTo().window(runWindows);
                DriverManager.getDriver().close();
            }
        }
        DriverManager.getDriver().switchTo().window(parentID);
    }

    public By getLocator(String locator) {
        By by = null;
        if (locator.startsWith("id=")) {
            by = by.id(locator.substring(3));
        } else if (locator.startsWith("name=")) {
            by = by.name(locator.substring(5));
        } else if (locator.startsWith("class=")) {
            by = by.className(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            by = by.cssSelector(locator.substring(4));
        } else if (locator.startsWith("xpath=")) {
            by = by.xpath(locator.substring(6));

        } else {
            throw new RuntimeException("Locator invalid");
        }
        return by;
    }

    public WebElement getElement(String locator) {
        return DriverManager.getDriver().findElement(getLocator(locator));
    }

    public WebElement getElement(String variableLocator, String... dynamicValues) {
        variableLocator = getLocatorDynamic(variableLocator, dynamicValues);
        return DriverManager.getDriver().findElement(getLocator(variableLocator));
    }

    public String getLocatorDynamic(String variableLocator, String... dynamicValues) {
        if (dynamicValues.length > 0) {
            variableLocator = String.format(variableLocator, (Object[]) dynamicValues);
        }
        return variableLocator;
    }

    public String getElementAttribute(String locator, String nameAttribute) {
        waitForElementPresence(locator);
        return getElement(locator).getAttribute(nameAttribute);
    }

    public String getElementAttribute(String locator, String nameAttribute, String... dynamicValues) {
        waitForElementPresence(locator, dynamicValues);
        return getElement(locator, dynamicValues).getAttribute(nameAttribute);
    }

    public String getElementValueCss(String locator, String propertyName) {
        return getElement(locator).getCssValue(propertyName);
    }

    public String getHexaColorFromRGBA(String rgbaValue) {
        return Color.fromString(rgbaValue).asHex();
    }

    public List<WebElement> getListElement(String locator) {
        return DriverManager.getDriver().findElements(getLocator(locator));
    }

    public List<WebElement> getListElement(String variableLocator, String... dynamicValues) {
        return DriverManager.getDriver().findElements(getLocator(getLocatorDynamic(variableLocator, dynamicValues)));
    }

    public int getElementSize(String locator) {
        return getListElement(locator).size();
    }

    public int getElementSize(String variableLocator, String... dynamicValues) {
        variableLocator = getLocatorDynamic(variableLocator, dynamicValues);
        return getListElement(variableLocator, dynamicValues).size();
    }

    public void clickToElement(String locator) {
        waitForElementClickable(locator);
        retryAction(() -> {
            getElement(locator).click();
        }, "Click to element");
    }

    public void clickToElement(String variableLocator, String... dynamicValues) {
        waitForElementClickable(variableLocator, dynamicValues);
        retryAction(() -> {
            getElement(variableLocator, dynamicValues).click();
        }, "Click to element");
    }

    public void sendKeysToElement(String locator, String textSendkey) {
        waitForElementVisible(locator);
        getElement(locator).clear();
        getElement(locator).sendKeys(textSendkey.trim());
    }

    public void sendKeysToElement(String locator, String textSendkey, String... dynamicValues) {
        waitForElementVisible(locator, dynamicValues);
        retryAction(() -> {
            getElement(locator, dynamicValues).clear();
            getElement(locator, dynamicValues).sendKeys(textSendkey.trim());
        }, "Send keys to element");
    }

    public void sendKeysToElementAndPressEnter(String locator, String textSendkey, String... dynamicValues) {
        sendKeysToElement(locator, textSendkey, dynamicValues);
        pressEnter();
    }

    public void sendKeysToElementByBackSpace(String locator, String textSenkey, String... dynamicValues) {
        waitForElementVisible(locator, dynamicValues);
        // Dùng phím BACK_SPACE để xóa từng ký tự
        WebElement element = getElement(locator, dynamicValues);
        for (int i = 0; i < 3; i++) {
            element.sendKeys(Keys.BACK_SPACE);
        }
        element.sendKeys(textSenkey.trim());
    }

    public String getElementText(String locator) {
        waitForElementVisible(locator);
        return getElement(locator).getText();
    }

    public String getElementText(String variableLocator, String... dynamicLocator) {
        waitForElementVisible(variableLocator, dynamicLocator);
        variableLocator = getLocatorDynamic(variableLocator, dynamicLocator);
        return getElement(variableLocator).getText().trim();
    }

    public void selectItemInDefaultDropdown(String locator, String textItem) {
        Select select = new Select(getElement(locator));
        select.selectByValue(textItem);
    }

    public String getSelectedItemInDefaultDropdown(String locator) {
        Select select = new Select(getElement(locator));
        return select.getFirstSelectedOption().getText();
    }

    public boolean isDropdownMultiple(String locator) {
        Select select = new Select(getElement(locator));
        return select.isMultiple();
    }

    public void selectItemCustomDropDown(String xpathParent, String xpathChild) {
        getElement(xpathParent).click();
        sleepInSecond(2);
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.presenceOfElementLocated(getLocator(xpathChild)));
        List<WebElement> allItem = DriverManager.getDriver().findElements(getLocator(xpathChild));
        for (WebElement item : allItem) {
            JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);", item);
            item.click();
            sleepInSecond(3);
            break;
        }
    }

    public List<String> getItemCustomDropDown(String parentXpath, String childXpath, String... dymParent) {
        Set<String> setDataDropDown = new LinkedHashSet<>();

        getElement(parentXpath, dymParent).click();
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        List<WebElement> allItems = explicitWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(getLocator(childXpath)));
        List<WebElement> allItemBefore = new ArrayList<>();
        boolean check = true;
        int dem = 0;
        while (check && dem < 3) {
            int i = 0;
            boolean checkDKScroll = false;
            if (dem > 0) {
                allItems.removeAll(allItemBefore);
                if (allItems.size() > 1) {
                    checkDKScroll = true;
                }
            } else {
                if (allItems.size() > 7) {
                    checkDKScroll = true;
                }
            }
            for (WebElement item : allItems) {

                if (checkDKScroll && i == allItems.size() - 1) {
                    JavascriptExecutor jsExcutor = (JavascriptExecutor) DriverManager.getDriver();
                    jsExcutor.executeScript("arguments[0].scrollIntoView(true);", item);
                    sleepInSecond(1);
                    check = true;
                } else {
                    check = false;
                }
                setDataDropDown.add(item.getAttribute("innerText").trim());
                i++;
            }
            allItemBefore = allItems;
            if (check) {
                allItems = explicitWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(getLocator(childXpath)));
            }
            dem++;
        }
        scrollToElement(getLocatorDynamic(parentXpath, dymParent));
        sleepInSecond(1);
        return new ArrayList<>(setDataDropDown);
    }

    public void selectItemCustomDropDown(String parentXpath, String childXpath, String expectedText, String... dymParent) {
        getElement(parentXpath, dymParent).click();
        sleepInSecond(1);
        WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));

        By childLocator = getLocator(childXpath);
        JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
        String expected = expectedText.trim();

        List<WebElement> allItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(childLocator));
        boolean isNotFound = true;
        int retryCount = 0;

        while (isNotFound && retryCount < 3) {
            for (WebElement item : allItems) {

                if (allItems.size() > 7) jsExecutor.executeScript("arguments[0].scrollIntoView(true);", item);

                if (item.getText().trim().equals(expected)) {
                    sleepInSecond(1);
                    item.click();
                    isNotFound = false;
                    break;
                }
            }

            if (isNotFound) {
                allItems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(childLocator));
            }
            retryCount++;
        }
    }

    public void checkToDefaultCheckbox(String locator, String... dymValues) {
        waitForElementPresence(locator, dymValues);
        retryAction(() -> {
            WebElement element = getElement(locator, dymValues);
            if (!element.isSelected()) element.click();

            if (!element.isSelected()) {
                throw new RuntimeException("Checkbox still not selected: " + getLocatorDynamic(locator, dymValues));
            }
        }, "Check to default checkbox");
    }

    public void uncheckToDefaultCheckbox(String locator, String... dymValues) {
        waitForElementPresence(locator, dymValues);
        retryAction(() -> {
            WebElement element = getElement(locator, dymValues);
            if (element.isSelected()) element.click();

            if (element.isSelected()) {
                errorMsg("Checkbox still selected: {}", getLocatorDynamic(locator, dymValues));
            }
        }, "Uncheck default checkbox");
    }


    public boolean isElementSelected(String locator, String... dymValues) {
        return getElement(locator, dymValues).isSelected();
    }

    public boolean isElementDisplayed(String locator, String... dymValues) {
        return getElement(locator, dymValues).isDisplayed();
    }

    public boolean isElementEnabled(String locator, String... dymValues) {
        return getElement(locator, dymValues).isEnabled();
    }

    public void switchToFrameIframe(String locator) {
        DriverManager.getDriver().switchTo().frame(getElement(locator));
    }

    public void switchToDefaultContent(String locator) {
        DriverManager.getDriver().switchTo().defaultContent();
    }

    public void hoverMouseToElement(String locator) {
        Actions action = new Actions(DriverManager.getDriver());
        action.moveToElement(getElement(locator)).perform();
    }

    public void pressEnter() {
        Actions action = new Actions(DriverManager.getDriver());
        action.sendKeys(Keys.ENTER).build().perform();
    }

    public Object executeForBrowser(String javaScript) {
        return ((JavascriptExecutor) DriverManager.getDriver()).executeScript(javaScript);
    }

    public String getInnerText(String locator) {
        return getElement(locator).getAttribute("textContent").trim();
    }

    public boolean areExpectedTextInInnerText(String textExpected) {
        String textActual = (String) ((JavascriptExecutor) DriverManager.getDriver())
                .executeScript("return document.documentElement.innerText.match('" + textExpected + "')[0]");
        return textActual.equals(textExpected);
    }

    public void scrollToBottomPage() {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollBy(0,document.body.scrollHeight)");
    }

    public void navigateToUrlByJS(String url) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.location = '" + url + "'");
    }

    public void highlightElement(String locator) {
        WebElement element = getElement(locator);
        String originalStyle = element.getAttribute("style");
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].setAttribute(arguments[1], arguments[2])", element,
                "style", "border: 2px solid red; border-style: dashed;");
        sleepInSecond(1);
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].setAttribute(arguments[1], arguments[2])", element,
                "style", originalStyle);
    }

    public void clickToElementByJS(String locator) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].click();", getElement(locator));
    }

    public void clickToElementByJS(String locator, String... dynamicLocator) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].click();", getElement(locator, dynamicLocator));
    }

    public void scrollToElement(String locator, String... dynamicLocator) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].scrollIntoView(true);", getElement(locator, dynamicLocator));
    }

    public void sendKeysToElementByJS(String locator, String value) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].setAttribute('value', '" + value + "')",
                getElement(locator));
    }

    public void scrollItemCustomDropDown(String childXpath, String expectedText, String... dymParent) {
        sleepInSecond(1);
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        List<WebElement> allItems = explicitWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(getLocator(childXpath)));
        boolean check = true;
        int dem = 0;
        while (check && dem < 3) {
            for (WebElement item : allItems) {
                if (allItems.size() > 7) {
                    JavascriptExecutor jsExcutor = (JavascriptExecutor) DriverManager.getDriver();
                    jsExcutor.executeScript("arguments[0].scrollIntoView(true);", item);
                }
                if (item.getText().trim().equals(expectedText.trim())) {
                    sleepInSecond(1);
//                    item.click();
                    check = false;
                    break;
                }
            }
            if (check) {
                allItems = explicitWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(getLocator(childXpath)));
            }
            dem++;
        }
    }

    public void scrollToElementInAntDesignTree(String listItemScroll, String expectedText, String... dymValue) {
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

        for (int i = 0; i < 20; i++) {
            List<WebElement> items = getListElement(listItemScroll, dymValue);

            for (WebElement item : items) {
                String text = item.getText().trim();
                if (text.equalsIgnoreCase(expectedText.trim())) {
                    js.executeScript("arguments[0].scrollIntoView(true);", item);
                    sleepInSecond(1);
                    return;
                }
            }
            // Nếu chưa thấy thì scroll xuống thêm
            js.executeScript("arguments[0].scrollTop = arguments[0].scrollTop + 250;", getElement("css=div.ant-tree-list-holder"));
            sleepInSecond(1); // chờ UI update
        }

        throw new RuntimeException("Không tìm thấy '" + expectedText + "' trong dropdown.");
    }


    public void setAttributeInDom(String locator, String attribute, String valueAttribute) {
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
        js.executeScript("arguments[0].setAttribute('" + attribute + "', '" + valueAttribute + "');", getElement(locator));
    }

    public void setAttributeInDom(String locator, String attribute, String valueAttribute, String... dymLocator) {
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
        js.executeScript("arguments[0].setAttribute('" + attribute + "', '" + valueAttribute + "');", getElement(getLocatorDynamic(locator, dymLocator)));
    }

    public void cleanTextByJavaScript(String locator, String... dymLocator) {
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
        js.executeScript("arguments[0].value = '';", getElement(getLocatorDynamic(locator, dymLocator)));
    }

    public void uploadMultipleFiles(String... fileName) {
        String filePath = GlobalConstants.PROJECT_PATH;
        String fullFileName = "";
        for (String file : fileName) {
            fullFileName = fullFileName + filePath + file + "\n";
        }
        fullFileName = fullFileName.trim();
        getElement("xpath=//input[@type='file']").sendKeys(fullFileName);
    }

    public void removeAttributeInDOM(String locator, String attributeRemove) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].removeAttribute('" + attributeRemove + "');",
                getElement(locator));
    }

    public void removeAttributeInDOM(String locator, String attributeRemove, String... dynamicLocator) {
        ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].removeAttribute('" + attributeRemove + "');",
                getElement(getLocatorDynamic(locator, dynamicLocator)));
    }

    public boolean areJQueryAndJSLoadedSuccess() {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return ((Long) ((JavascriptExecutor) driver).executeScript("return jQuery.active") == 0);
                } catch (Exception e) {
                    return true;
                }
            }
        };
        ExpectedCondition<Boolean> jsLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return ((JavascriptExecutor) driver).executeScript("return document.readyState").toString()
                        .equals("complete");
            }
        };
        return explicitWait.until(jQueryLoad) && explicitWait.until(jsLoad);
    }

    public boolean isJQueryAndJSLoadedSuccess() {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
        ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return (Boolean) jsExecutor.executeScript("return (window.jQuery != null) && (jQuery.active==0); ");
            }
        };
        return explicitWait.until(jQueryLoad);
    }

    public String getElementValidationMessage(String locator) {
        return (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return arguments[0].validationMessage;",
                getElement(locator));
    }

    public boolean isImageLoaded(WebDriver driver, String locator) {
        boolean status = (boolean) ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].complete && typeof arguments[0].naturalWidth != \"undefined\" && arguments[0].naturalWidth > 0",
                getElement(locator));
        if (status) {
            return true;
        } else {
            return false;
        }
    }

    public void waitForElementVisible(String locator) {
        WebDriverWait explicitwait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitwait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(locator)));
    }

    public void waitForElementVisible(String variableLocator, String... dynamicValues) {
        WebDriverWait explicitwait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitwait.until(ExpectedConditions
                .visibilityOfElementLocated(getLocator(getLocatorDynamic(variableLocator, dynamicValues))));
    }

    public void waitForAllElementVisible(String locator) {
        WebDriverWait explicitwait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitwait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(getLocator(locator)));
    }

    public void waitForElementInvisible(String locator, String... dynamicValues) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.invisibilityOfElementLocated(getLocator(getLocatorDynamic(locator, dynamicValues))));
    }

    public void waitForElementInvisible(String locator) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.invisibilityOfElementLocated(getLocator(locator)));
    }

    public void waitForAllElementInvisible(String locator) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.invisibilityOfAllElements(getListElement(locator)));
    }

    public void waitForElementClickable(String locator) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.elementToBeClickable(getLocator(locator)));
    }

    public void waitForElementClickable(String variableLocator, String... dynamicValues) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.elementToBeClickable(getLocator(getLocatorDynamic(variableLocator, dynamicValues))));
    }

    public void waitForElementPresence(String variableLocator, String... dynamicValues) {
        WebDriverWait explicitWait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        explicitWait.until(ExpectedConditions.presenceOfElementLocated(getLocator(getLocatorDynamic(variableLocator, dynamicValues))));
    }

    /**
     * Wait for a page to load with the default time from the config
     */
    public void waitForPageLoaded() {
        WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
        ExpectedCondition<Boolean> jsLoad = driver1 -> ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return document.readyState").toString().equals("complete");
        boolean jsReady = js.executeScript("return document.readyState").toString().equals("complete");
        if (!jsReady) {
            LogUtils.info("Javascript in NOT Ready!");
            try {
                wait.until(jsLoad);
            } catch (Throwable error) {
                error.printStackTrace();
                Assert.fail("Timeout waiting for page load. (" + 30 + "s)");
            }
        }
    }

    public static void sleepInSecond(long second) {
        try {
            Thread.sleep(1000 * second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getDateTimeNow() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime futureTime = currentDateTime.plusMinutes(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");
        return futureTime.format(formatter);
    }

    public String getDateTomorrow() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime futureTime = currentDateTime.plusDays(360);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return futureTime.format(formatter);
    }

    public static String getDateTimeNow(String pattern) {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(pattern));
    }

    public int randomNumber() {
        return randomNumber(100000000);
    }

    public int randomNumber(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound phải lớn hơn 0");
        }
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static String[] getArrayAfterPhanTachDauPhay(String text) {
        text = text.trim();
        String textAfterReplace = text.replace("[", "").replace("]", "").trim();
        return textAfterReplace.split(",");
    }

    public static String[] getArrayAfterPhanTachDauSoc0(String text) {
        text = text.trim();
        String textAfterReplace = text.replace("[", "").replace("]", "").trim();
        return textAfterReplace.split("\\|");
    }

    public static String[] getArrayAfterPhanTachDauSoc(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }

        String textAfterReplace = text.replace("[", "").replace("]", "").trim();

        return Arrays.stream(textAfterReplace.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public static String[] getArrayAfterPhanTachDauHaiCham(String text) {
        text = text.trim();
        String textAfterReplace = text.replace("[", "").replace("]", "").trim();
        return textAfterReplace.split(":");
    }

    public static String[] getArrayAfterPhanTachDauChamPhay(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }

        text = text.trim();
        String textAfterReplace = text.replace("[", "").replace("]", "").trim();

        // split theo dấu chấm phẩy và loại bỏ phần tử rỗng
        return Arrays.stream(textAfterReplace.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public static String[] getArrayAfterPhanTachDauPhay0(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }

        String textAfterReplace = text.replace("[", "").replace("]", "").trim();

        return Arrays.stream(textAfterReplace.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }


    public static String[] splitByCommaOrPipe(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }
        text = text.trim();
        String textAfterReplace = text.replace("[", "").replace("]", "").trim();
        return textAfterReplace.split("\\s*[|,]\\s*");
    }

    public static boolean isContainInArray(String[] myArray, String valueToFind) {
        boolean isContainInArray = false;
        for (int i = 0; i < myArray.length; i++) {
            if (myArray[i].equals(valueToFind)) {
                isContainInArray = true;
                break;
            }
        }
        return isContainInArray;
    }

    public static boolean isJsonArrayContainSmallArray(com.fasterxml.jackson.databind.JsonNode jsonArray, String[] smallArray) {
        return JsonHelper.isJsonArrayContainSmallArray(jsonArray, smallArray);
    }

    public static boolean isJsonArrayContainSmallArray(String jsonArrayString, String[] smallArray) {
        return JsonHelper.isJsonArrayContainSmallArray(jsonArrayString, smallArray);
    }

    public static void logInfo(String message, Object... args) {
        String msg = formatMessage(message, args);
        LogUtils.info(msg);
        Allure.step(msg);
    }

    // Đánh FAIL testcase
    public static boolean logError(String error, Object... args) {
        try {
            String msg = formatMessage(error, args);
            String prefix = "[ERROR] " + getDateTimeNow("dd-MM-yyyy HH:mm:ss") + " - ";

            System.err.println(prefix + "❌ " + msg);
            Allure.step(msg, Status.FAILED);

            AssertionError assertionError = new AssertionError(msg);
            var result = Reporter.getCurrentTestResult();
            VerificationFailures.getFailures().addFailureForTest(result, assertionError);
            result.setThrowable(assertionError);

        } catch (Exception ignored) {
        }
        return false;
    }

    //Không đánh FAIL testcase
    public static void errorMsg(String error, Object... args) {
        String msg = formatMessage(error, args);
        String prefix = "[ERROR] " + getDateTimeNow("dd-MM-yyyy HH:mm:ss") + " - ";
        System.err.println(prefix + "❌ " + msg);
        Allure.step(msg, Status.FAILED);
    }


    public static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) return message;

        for (Object arg : args) {
            message = message.replaceFirst("\\{}", arg == null ? "null" : arg.toString());
        }
        return message;
    }

    public static boolean compareTwoValues(String actualValue, String expectedValue) {
        return Objects.equals(actualValue.trim(), expectedValue.trim());
    }


    public boolean isElementExist(String locator) {
        return getElementSize(locator) > 0;
    }

    public boolean isElementExist(String locator, String... dynamicValues) {
        return getElementSize(locator, dynamicValues) > 0;
    }

    public static <T extends Enum<T>> String getNameEnum(T enumValue) {
        if (enumValue == null) {
            return null;
        }
        return enumValue.name();
    }

    // Hàm so sánh nhiều cột bất kỳ và log đầy đủ
    public static void compareColumnsStrictVerbose(SQLiteHelper sqLiteHelper, int rowIndex, List<String> failedCols, String label, String failFormat, String... columnsOrValues) {
        if (columnsOrValues == null || columnsOrValues.length < 2) {
            throw new IllegalArgumentException("Phải có ít nhất 2 giá trị để so sánh");
        }
        List<String> values = new ArrayList<>();
        List<String> sources = new ArrayList<>();

        for (String colOrVal : columnsOrValues) {
            String value = "";
            if (colOrVal != null && colOrVal.startsWith("value=")) {
                value = colOrVal.substring(6);
            } else {
                value = Optional.ofNullable(sqLiteHelper.getCellData(colOrVal, rowIndex)).orElse("");
            }
            values.add(value);
            sources.add(colOrVal.startsWith("value=") ? "(expected)" : colOrVal);
        }

        String firstVal = values.get(0);
        boolean allEqual = true;
        StringBuilder diffLog = new StringBuilder();

        for (int i = 1; i < values.size(); i++) {
            String currentVal = values.get(i);
            if (!compareTwoValues(firstVal, currentVal)) {
                allEqual = false;
                diffLog.append(String.format(" - Khác tại [%s]: %s vs [%s]: %s\n",
                        sources.get(0), firstVal, sources.get(i), currentVal));
            }
        }

        if (!allEqual) {
            failedCols.add(label);
            errorMsg(String.format("FAIL: %s 📛 " + failFormat, prependToFormat(label, values)));
        } else {
            logInfo(String.format("   ├─ ✅ PASS: %s 👉 Value: %s", label, firstVal));
        }
    }

    private static Object[] prependToFormat(String label, List<String> values) {
        Object[] result = new Object[values.size() + 1];
        result[0] = label;
        for (int i = 0; i < values.size(); i++) {
            result[i + 1] = values.get(i);
        }
        return result;
    }


    public static String getFileName(String filePath) {
        return Optional.ofNullable(filePath).filter(f -> !f.isEmpty()).map(Paths::get).map(Path::getFileName).map(Path::toString).map(name -> name.replaceFirst("\\.[^.]+$", "")).orElse("");
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public static String appendWithComma(String sb, String value) {
        if (value == null || value.isEmpty()) return sb;
        return sb.isEmpty() ? value : sb + ", " + value;
    }

    public static String appendWithPipe(String sb, String value) {
        if (value == null || value.isEmpty()) return sb;
        return sb.isEmpty() ? value : sb + "|" + value;
    }

    public static String duplicateValuesByLength(SQLiteHelper sqLiteHelper, String column, String value, int i) {
        String finalValue = "";
        String[] listData = getArrayAfterPhanTachDauPhay(sqLiteHelper.getCellData(column, i));
        if (listData != null && listData.length > 0 && !(listData.length == 1 && listData[0].trim().isEmpty())) {
            finalValue = String.join(", ", Collections.nCopies(listData.length, value));
        }
        return finalValue;
    }

    public static boolean verifyTrue(boolean condition, String mess) {
        boolean pass = true;
        try {
            Assert.assertTrue(condition, mess);
            LogUtils.info(mess + " | PASSED");
            Allure.step(mess + " | PASSED", Status.PASSED);
        } catch (Throwable e) {
            LogUtils.info(mess + " | FAILED ");
            setStepFail(mess);
            pass = false;
            VerificationFailures.getFailures().addFailureForTest(Reporter.getCurrentTestResult(), e);
            Reporter.getCurrentTestResult().setThrowable(e);
        }
        return pass;
    }

    private static void setStepFail(String message) {
        String uuid = java.util.UUID.randomUUID().toString();
        StepResult stepResult = new StepResult().setName(message);
        Allure.getLifecycle().startStep(uuid, stepResult);
        Allure.getLifecycle().updateStep(uuid, result -> result.setStatus(Status.FAILED));
        captureScreenshot();
        try {
            if (DriverManager.getDriver() != null) {
                String pageSource = DriverManager.getDriver().getPageSource();
                Allure.addAttachment("Page Source", "text/html", pageSource, ".html");
            }
        } catch (Exception ignored) {
        }
        Allure.getLifecycle().stopStep(uuid);
    }

    public boolean verifyFalse(boolean condition, String mess) {
        boolean pass = true;
        try {
            Assert.assertFalse(condition, mess);
            LogUtils.info(mess + " | PASSED");
            Allure.step(mess + " | PASSED", Status.PASSED);
        } catch (Throwable e) {
            LogUtils.info(mess);
            setStepFail(mess);
            pass = false;
            LogUtils.info(mess + " | FAILED");
            VerificationFailures.getFailures().addFailureForTest(Reporter.getCurrentTestResult(), e);
            Reporter.getCurrentTestResult().setThrowable(e);
        }
        return pass;
    }

    public boolean verifyEquals(Object actual, Object expected, String mess) {
        boolean pass = true;
        try {
            Assert.assertEquals(actual, expected, mess);
            LogUtils.info("actual: " + actual + " == expected: " + expected + " | PASSED");
            Allure.step(mess + " | PASSED", Status.PASSED);
        } catch (Throwable e) {
            pass = false;
            LogUtils.info("actual: " + actual + " != expected: " + expected + " | FAILED");
            setStepFail(mess);
            VerificationFailures.getFailures().addFailureForTest(Reporter.getCurrentTestResult(), e);
            Reporter.getCurrentTestResult().setThrowable(e);
        }
        return pass;
    }


    public static void captureScreenshot() {
        if (DriverManager.getDriver() == null) return;

        try {
            byte[] screenshotBytes = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Screenshot", new ByteArrayInputStream(screenshotBytes));   // Attach to Allure
            String fileName = getDateTimeNow("yyyy-MM-dd_HH-mm-ss-SSS") + ".png";
            Path outputPath = Paths.get(GlobalConstants.PROJECT_PATH, "allure-results", fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, screenshotBytes);
            System.out.println("📸 Screenshot captured: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Capture screenshot failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    public static boolean isValueNullOrEmpty(String value) {
//        return value == null || value.isEmpty();
//    }


    public static boolean isValueNullOrEmpty(Object value) {
        return switch (value) {
            case null -> true;
            case String str -> str.trim().isEmpty();
            case Collection<?> collection ->
                    collection.isEmpty(); //support: List<Integer>, List<String>, Set<Long>, HashSet<User>
            case Map<?, ?> map -> map.isEmpty();
            case Object[] array -> array.length == 0;
            default -> false;
        };
    }

    public static boolean isValueNotNullOrEmpty(Object value) {
        return !isValueNullOrEmpty(value);
    }

    public static String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (Exception ignored) {
            logInfo("Failed to reading file path: {}", filePath);
            return null;
        }
    }



}
