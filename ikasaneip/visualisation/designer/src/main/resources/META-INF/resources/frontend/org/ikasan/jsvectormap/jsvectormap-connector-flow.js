window.Vaadin.Flow.jsvectormap = {

    initLazy : function(designer, name, readonly) {

        // Check whether the connector was already initialized for the Iron list
        if (designer.$connector) {
            return;
        }
        console.log('init jsvectormap');

        designer.$connector = {};

        designer.$connector.map = new jsVectorMap({
            // world_merc, us_mill_en, us_merc_en,
            // us_lcc_en, us_aea_en, spain
            // russia, canada, iraq
            map: 'world',
            selector: '#ikasan-world-map',
            backgroundColor: 'tranparent',
            draggable: true,
            zoomButtons: false,
            zoomOnScroll: false,
            zoomOnScrollSpeed: 3,
            zoomMax: 12,
            zoomMin: 1,
            zoomAnimate: true,
            showTooltip: true,
            zoomStep: 1.5,
            bindTouchEvents: true,
            // Line options
            lineStyle: {
                stroke: '#808080',
                strokeWidth: 1,
                strokeLinecap: 'round'
            },
            focusOn: {}, // focus on regions on page load
            /**
             * Markers options
             */
            markers: null, // Set of markers to add to the map during initialization
            markersSelectable: false,
            markersSelectableOne: false,
            markerStyle: {
                initial: {
                    // r: 7,
                    // fill: '#374151',
                    // fillOpacity: 1,
                    // stroke: '#FFF',
                    // strokeWidth: 5,
                    // strokeOpacity: .5,
                    image: "frontend/images/mr_squid_curtailed_logo.png"
                },
                hover: {
                    // fill: '#3cc0ff',
                    cursor: 'pointer'
                },
                selected: {
                    fill: 'blue'
                },
                selectedHover: {}
            },
            markerLabelStyle: {
                initial: {
                    fontFamily: 'Verdana',
                    fontSize: 12,
                    fontWeight: 500,
                    cursor: 'default',
                    fill: '#374151'
                },
                hover: {
                    cursor: 'pointer'
                },
                selected: {},
                selectedHover: {}
            },
            lineStyle: {
                stroke: "#676767",
                strokeWidth: 1.5,
                fill: '#ff5566',
                fillOpacity: 1,
                strokeDasharray: '6 3 6', // OR: [6, 2, 6]
                animation: true // Enables animation
            },
            /**
             * Region styles
             */
            labels: { // add a label for a specific region
                // regions: {
                //     render(code) {
                //         return ['EG', 'KZ', 'CN'].indexOf(code) > -1 ? 'Hello ' + code : ''
                //     },
                // },
                markers: {
                    // Starting from jsvectormap v1.2 the render function receives
                    // the marker object as a first parameter and index as the second.
                    render(marker, index) {
                        return marker.name || marker.labelName || 'Not available'
                    }
                }
            },
            regionsSelectable: false,
            regionsSelectableOne: false,
            regionStyle: {
                // Region style
                initial: {
                    fill: '#e3eaef',
                    fillOpacity: 1,
                    stroke: 'none',
                    strokeWidth: 0,
                    strokeOpacity: 1
                },
                hover: {
                    fillOpacity: .7,
                    // cursor: 'pointer'
                },
                selected: {
                    fill: '#D3D3D3'
                },
                selectedHover: {}
            },
            // Region label style
            regionLabelStyle: {
                initial: {
                    fontFamily: 'Verdana',
                    fontSize: '12',
                    fontWeight: 'bold',
                    cursor: 'default',
                    fill: '#35373e'
                },
                hover: {
                    cursor: 'pointer'
                }
            },
            series: {
                markers: [
                    // You can add one or more objects to create series for markers.
                ],
                regions: [
                    // You can add one or more objects to create series for regions.
                ]
            },
            // // map visualization is used to analyze and display the geographically related data and present it in the form of maps.
            // visualizeData: {
            //     scale: ['#eeeeee', '#D3D3D3'],
            //     values: {
            //         GB: 100,
            //         US: 100,
            //         JP: 100,
            //         HK: 100,
            //         SG: 100,
            //         DE: 100,
            //         // ...
            //     }
            // }

            selectedRegions: ['GB', 'US', 'JP', 'HK', 'SG', 'DE'],

            onMarkerSelected: function (code, isSelected, selectedMarkers) {
                debugger;
                console.log(code, isSelected, selectedMarkers);
                let element = document.getElementById("ikasan-world-map");
                element.$server.regionSelected(code);
            },

            onMarkerClick: function (code, markerIndex) {
                debugger;
                console.log(code, markerIndex);
                let element = document.getElementById("ikasan-world-map");
                element.$server.regionSelected(markerIndex);
            },
        });

        designer.$connector.map.addMarkers({ name: 'Tokyo', coords: [35.6762, 139.6503] });
        designer.$connector.map.addMarkers({ name: 'New York', coords: [40.7128, -74.0060] });
        designer.$connector.map.addMarkers({ name: 'London', coords: [51.5072, 0.1276] });
        designer.$connector.map.addMarkers({ name: 'Frankfurt', coords: [50.1109, 8.6821] });
        designer.$connector.map.addMarkers({ name: 'Singapore', coords: [1.3521, 103.8198] });
        designer.$connector.map.addMarkers({ name: 'Hong Kong', coords: [22.3193, 114.1694] });

        designer.$connector.map.addLines([
            { from: 'Tokyo', to: 'Frankfurt' },
            { from: 'Frankfurt', to: 'Tokyo' },
            { from: 'London', to: 'New York' },
            { from: 'New York', to: 'London' },
            { from: 'London', to: 'Tokyo' },
            { from: 'Tokyo', to: 'London' },
            { from: 'Tokyo', to: 'New York' },
            { from: 'New York', to: 'Tokyo' },
            { from: 'London', to: 'Frankfurt' },
            { from: 'Frankfurt', to: 'London' },
            { from: 'Singapore', to: 'Tokyo' },
            { from: 'Tokyo', to: 'Singapore' },
            { from: 'Hong Kong', to: 'Tokyo' },
            { from: 'Tokyo', to: 'Hong Kong' },
            { from: 'Hong Kong', to: 'Singapore' },
            { from: 'Singapore', to: 'Hong Kong' },
            { from: 'Hong Kong', to: 'New York' },
            { from: 'New York', to: 'Hong Kong' },
            { from: 'Singapore', to: 'New York' },
            { from: 'New York', to: 'Singapore' },
            { from: 'Singapore', to: 'London' },
            { from: 'London', to: 'Singapore' },
            { from: 'Hong Kong', to: 'London' },
            { from: 'London', to: 'Hong Kong' },
            { from: 'Singapore', to: 'Frankfurt' },
            { from: 'Frankfurt', to: 'Singapore' },
            { from: 'Hong Kong', to: 'Frankfurt' },
            { from: 'Frankfurt', to: 'Hong Kong' },
            { from: 'New York', to: 'Frankfurt' },
            { from: 'Frankfurt', to: 'New York' },
        ])

        debugger;
        console.log(designer.$connector.map);
    }
}
