// Preload FontAwesome iconsets to fix Chrome rendering issue
// This ensures icons load before they're needed by components

(function() {
    // Check if already loaded
    if (window.__fontAwesomeLoaded) {
        return;
    }

    // Import the iconsets
    import('./generated/jar-resources/font-awesome-iron-iconset/far.js');
    import('./generated/jar-resources/font-awesome-iron-iconset/fas.js');

    // Mark as loaded
    window.__fontAwesomeLoaded = true;
})();
