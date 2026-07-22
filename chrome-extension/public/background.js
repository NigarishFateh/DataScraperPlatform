/**
 * MV3 service worker (plain JS in /public so Vite copies it unchanged).
 * Clicking the toolbar action opens the Side Panel — never a popup.
 */
chrome.runtime.onInstalled.addListener(() => {
  chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
});

chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
