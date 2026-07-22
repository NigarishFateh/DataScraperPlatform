/**
 * TypeScript source mirror of public/background.js.
 * Kept for documentation / future bundling. The packaged worker is public/background.js.
 */
chrome.runtime.onInstalled.addListener(() => {
  void chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
});

void chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
