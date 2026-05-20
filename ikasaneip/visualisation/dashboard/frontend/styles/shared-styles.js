import '@polymer/polymer/lib/elements/custom-style.js';

const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<custom-style>
    <style>
        html {
            --aura-primary-color-10pct: rgba(241, 90, 35, 0.1);
            --aura-primary-color-50pct: rgba(241, 90, 35, 0.5);
            --aura-accent-color: rgba(241, 90, 35, 1.0);
            --aura-primary-contrast-color: #FFF;
            --aura-font-family: "Arial Bold", sans-serif;
        }

        vaadin-grid {

            --aura-size-xs: 10px;
            --aura-size-s: 12px;
            --aura-size-m: 14px;
            --aura-size-l: 16px;
            --aura-size-xl: 20px;
            /* Font sizes (will affect spacing also) */
            --aura-font-size-xxs: 9px;
            --aura-font-size-xs: 10px;
            --aura-font-size-s: 11px;
            --aura-font-size-m: 12px;
            --aura-font-size-l: 13px;
            --aura-font-size-xl: 14px;
            --aura-font-size-xxl: 15px;
            --aura-font-size-xxxl: 19px;
            /* Icon size */
            --aura-icon-size-s: 1em;
            --aura-icon-size-m: 1.25em;
            --aura-icon-size-l: 1.5em;
            /* Line height */
            --aura-line-height-xs: 1.1;
            --aura-line-height-s: 1.3;
            --aura-line-height-m: 1.5;
        }

        vaadin-combo-box {

            --aura-size-xs: 20px;
            --aura-size-s: 24px;
            --aura-size-m: 28px;
            --aura-size-l: 32px;
            --aura-size-xl: 40px;
            /* Font sizes (will affect spacing also) */
            --aura-font-size-xxs: 10px;
            --aura-font-size-xs: 11px;
            --aura-font-size-s: 12px;
            --aura-font-size-m: 14px;
            --aura-font-size-l: 16px;
            --aura-font-size-xl: 20px;
            --aura-font-size-xxl: 24px;
            --aura-font-size-xxxl: 32px;
            /* Icon size */
            --aura-icon-size-s: 1em;
            --aura-icon-size-m: 1.25em;
            --aura-icon-size-l: 1.5em;
            /* Line height */
            --aura-line-height-xs: 1.1;
            --aura-line-height-s: 1.3;
            --aura-line-height-m: 1.5;
        }
        
        :host {
            --explorer-tree-grid-toggle-level-offset: 2rem;
            --explorer-tree-grid-icon-type-width: 1.5rem;
            --explorer-tree-grid-expand-icon-width: 1.2rem;
            --explorer-tree-grid-icon-type-margin: 0.1rem;
            --explorer-tree-grid-line-color: var(--aura-primary-color);
            --explorer-tree-grid-icon-color: var(--aura-primary-color-50pct);
            --explorer-tree-grid-icon-hover-color: var(--aura-primary-color);
            --explorer-tree-grid-border-style: solid;
        }

    </style>
</custom-style>

<custom-style>
    <template>
        <style include="material-color-light material-typography">

            :host,
            [theme~="dark"] {
                --material-primary-color: rgba(241, 90, 35, 0.5);
            }

        </style>

    </template>
</custom-style>

<dom-module theme-for="vaadin-grid" id="my-grid">
    <template>
        <style>
            [part~="row"]:hover [part~="body-cell"]{
                color: rgba(241, 90, 35, 1.0);;
                background-color: rgba(241, 90, 35, 0.1);
            }
        </style>
    </template>
</dom-module>

<dom-module theme-for="vaadin-text-field" id="ikasan-small">
    <template>
        <style>
            vaadin-text-field {

                --aura-size-xs: 10px;
                --aura-size-s: 12px;
                --aura-size-m: 14px;
                --aura-size-l: 16px;
                --aura-size-xl: 20px;
                /* Font sizes (will affect spacing also) */
                --aura-font-size-xxs: 7px;
                --aura-font-size-xs: 8px;
                --aura-font-size-s: 9px;
                --aura-font-size-m: 10px;
                --aura-font-size-l: 11px;
                --aura-font-size-xl: 13px;
                --aura-font-size-xxl: 15px;
                --aura-font-size-xxxl: 19px;
                /* Icon size */
                --aura-icon-size-s: 1em;
                --aura-icon-size-m: 1.25em;
                --aura-icon-size-l: 1.5em;
                /* Line height */
                --aura-line-height-xs: 1.1;
                --aura-line-height-s: 1.3;
                --aura-line-height-m: 1.5;
            }

        </style>
    </template>
</dom-module>

<dom-module id="dialog-fix" theme-for="vaadin-dialog-overlay">
    <template>
        <style>
            [part~="overlay"]{
                max-width: none !important;
                min-width: 0px !important;
            }
        </style>
    </template>
</dom-module>

<dom-module id="material-button-min-width" theme-for="vaadin-button">
  <template>
    <style>
      :host {
        min-width: 0px !important;
      }
      </style>
    </template>
</dom-module>

<dom-module id="ikasan-grid-styles" theme-for="vaadin-grid">
    <template>
        <style>
            
            [part~="header-cell"] {
                font-size: 11pt;
                height: 20px;
            }
            
            [part~="body-cell"] {
                font-size: 9pt;
                height: auto;
            }
            
            [part~="cell"]:not([part~="details-cell"]) {
                flex-shrink: 0;
                flex-grow: 1;
                box-sizing: border-box;
                display: flex;
                width: 100%;
                position: relative;
                align-items: start;
                padding: 0;
                white-space: nowrap;
            }
            
            /* Background needs a stronger selector to not be overridden */
            [part~="cell"].running {
                background-color: rgba(5,227,108, 0.3);
            }

            [part~="cell"].stoppedInError {
                background-color: rgba(255, 0, 0, 0.3);
            }

            [part~="cell"].recovering {
                background-color: rgba(253,185,19, 0.3);
            }

            [part~="cell"].paused {
                background-color: rgba(133,181,225, 0.3);
            }

            [part~="cell"].startPause {
                background-color: rgba(133,181,225, 0.3);
            }

            [part~="cell"].stopped {
                background-color: rgba(211,211,211, 0.3);
            }
            
            [part~="cell"].error {
               background-color: rgba(222, 40, 57, 0.3);
            }

        </style>


    </template>
</dom-module>`;

document.head.appendChild($_documentContainer.content);