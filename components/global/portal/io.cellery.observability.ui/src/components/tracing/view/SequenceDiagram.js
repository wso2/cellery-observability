/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import "./SequenceDiagram.css";
import ChevronRight from "@material-ui/icons/ChevronRight";
import Constants from "../../../utils/constants";
import React from "react";
import Span from "../../../utils/tracing/span";
import TracingUtils from "../../../utils/tracing/tracingUtils";
import Typography from "@material-ui/core/Typography";
import classNames from "classnames";
import interact from "interactjs";
import mermaid from "mermaid";
import {withStyles} from "@material-ui/core/styles";
import withColor, {ColorGenerator} from "../../common/color";
import * as PropTypes from "prop-types";

const styles = () => ({
    hidden: {
        display: "none"
    },
    sequenceDiagram: {
        padding: 10
    },
    breadcrumbs: {
        paddingTop: 20,
        paddingBottom: 5,
        marginLeft: 50
    },
    breadcrumbItemContainer: {
        display: "block",
        float: "left"
    },
    clickableBreadcrumbItem: {
        color: "#3e51b5",
        cursor: "pointer",
        textDecorationLine: "none"
    },
    breadcrumbItem: {
        fontWeight: 400,
        fontSize: "1.0rem"
    },
    clickableMessageText: {
        fill: "#4c4cb3 !important",
        cursor: "pointer"
    },
    tooltip: {
        display: "none",
        position: "fixed",
        background: "#f2f2f2",
        border: "1px solid #eee",
        borderRadius: 3,
        padding: 5,
        fontSize: 10,
        cursor: "pointer"
    }
});

class SequenceDiagram extends React.Component {

    static GLOBAL = "global";
    static GLOBAL_GATEWAY = `${SequenceDiagram.GLOBAL}-gateway`;
    static ACTION_LABEL_PATTERN = /Call\s([^\s]+)\s\[(\d+)]$/;

    static Classes = {
        ACTION_MESSAGE_TEXT: "messageText",
        ACTOR: "actor"
    };

    constructor(props) {
        super(props);
        this.state = {
            sequenceDiagramData: "",
            selectedCell: SequenceDiagram.GLOBAL,
            selectedActionId: ""
        };
        this.mermaidDivRef = React.createRef();
    }

    render() {
        const {classes} = this.props;
        const {selectedCell, selectedActionId, sequenceDiagramData} = this.state;
        return (
            <div>
                <div className={classes.breadcrumbs}>
                    <span className={classes.breadcrumbItemContainer}>
                        <Typography color={"textSecondary"} onClick={this.drawCellLevelSequenceDiagram}
                            className={classNames(classes.breadcrumbItem,
                                {[classes.clickableBreadcrumbItem]: Boolean(selectedActionId)})}>
                        Cells
                        </Typography>
                    </span>
                    <span className={classes.breadcrumbItemContainer}>
                        <ChevronRight className={classNames({[classes.hidden]: !selectedActionId})}
                            color={"action"}/>
                    </span>
                    <span className={classes.breadcrumbItemContainer}>
                        <Typography color={"textSecondary"}
                            className={classNames(classes.breadcrumbItem,
                                {[classes.hidden]: !selectedActionId})}>
                            Call {selectedCell} Cell [{selectedActionId}]
                        </Typography>
                    </span>
                </div>
                <br/>
                {
                    sequenceDiagramData
                        ? (
                            <div>
                                <div id="tooltip" className={classes.tooltip}></div>
                                <div className={classes.sequenceDiagram} ref={this.mermaidDivRef}>
                                    {sequenceDiagramData}
                                </div>
                            </div>
                        )
                        : null
                }
            </div>
        );
    }

    componentDidMount() {
        const {selectedActionId} = this.state;
        const self = this;
        self.drawCellLevelSequenceDiagram();
        interact(`.${SequenceDiagram.Classes.ACTION_MESSAGE_TEXT}`).on("tap", (event) => {
            if (event.srcElement.innerHTML.match(SequenceDiagram.ACTION_LABEL_PATTERN) && !selectedActionId) {
                const matches = (SequenceDiagram.ACTION_LABEL_PATTERN).exec(event.srcElement.innerHTML);
                this.setState({
                    selectedCell: matches[1]
                });
                this.drawComponentLevelSequenceDiagram(Number(matches[2]));
            }
        });
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        const {selectedActionId, sequenceDiagramData} = this.state;
        if (sequenceDiagramData !== prevState.sequenceDiagramData) {
            this.mermaidDivRef.current.removeAttribute("data-processed");
            mermaid.init(this.mermaidDivRef.current);

            if (!selectedActionId) {
                this.updateActionLabelStyle();
            }
        }
        this.updateActorStyle(selectedActionId);
    }

    /**
     * Update the action label's style.
     */
    updateActionLabelStyle() {
        const {classes} = this.props;
        const actionLabelElement = this.mermaidDivRef.current
            .getElementsByClassName(SequenceDiagram.Classes.ACTION_MESSAGE_TEXT);
        for (let i = 0; i < actionLabelElement.length; i++) {
            if (actionLabelElement[i].innerHTML.match(SequenceDiagram.ACTION_LABEL_PATTERN)) {
                actionLabelElement[i].classList.add(classes.clickableMessageText);
            }
        }
    }

    /**
     * Update the actor's style.
     *
     * @param {boolean} isComponentLevel True if the sequence diagram is a component level Sequence Diagram
     */
    updateActorStyle(isComponentLevel) {
        const {colorGenerator} = this.props;
        const {selectedCell} = this.state;
        const elementArray = this.mermaidDivRef.current.getElementsByClassName(SequenceDiagram.Classes.ACTOR);
        const elementWidth = elementArray[0].getAttribute("width");
        if (isComponentLevel) {
            const cellName = selectedCell;
            let color;
            if (cellName === SequenceDiagram.GLOBAL) {
                color = colorGenerator.getColor(ColorGenerator.SYSTEM);
            } else {
                color = colorGenerator.getColor(SequenceDiagram.getActorIdFromSanitizedName(cellName));
            }

            for (let i = 1; i < elementArray.length; i += 2) {
                const componentName = SequenceDiagram.getActorIdFromSanitizedName(
                    elementArray[i].firstElementChild.innerHTML);

                // Show and hide actor tooltip on hover to rectangle
                elementArray[i - 1].addEventListener("mouseenter", (event) => {
                    this.showTooltip(event, componentName);
                });
                elementArray[i - 1].addEventListener("mouseleave", () => {
                    this.hideTooltip();
                });

                // Show and hide actor tooltip on hover to text
                elementArray[i].addEventListener("mouseenter", (event) => {
                    this.showTooltip(event, componentName);
                });
                elementArray[i].addEventListener("mouseleave", () => {
                    this.hideTooltip();
                });

                // Truncating cell name when cellName is too long
                const letterLength = elementArray[i].getBBox().width / elementArray[i].textContent.length;
                const lettersLength = Math.round(elementWidth / letterLength);
                if (elementArray[i].getBBox().width > elementWidth) {
                    const truncatedComponentName = `${componentName.substring(0, lettersLength - 5)}...`;
                    elementArray[i].textContent = truncatedComponentName;
                }

                elementArray[i - 1].style.stroke = color;
                elementArray[i - 1].style.strokeWidth = 3;
                elementArray[i - 1].style.fill = "#ffffff";
            }
        } else {
            // For loop with iteration by factor 2 to skip SVG `rect` element and get the text in each actor.
            for (let i = 1; i < elementArray.length; i += 2) {
                if (elementArray[i].firstElementChild !== null) {
                    const cellName = SequenceDiagram.getActorIdFromSanitizedName(
                        elementArray[i].firstElementChild.innerHTML);
                    let color;
                    if (cellName === SequenceDiagram.GLOBAL_GATEWAY) {
                        color = colorGenerator.getColor(ColorGenerator.SYSTEM);
                    } else {
                        color = colorGenerator.getColor(cellName);
                    }
                    // Index of i-1 is given to set the style to the respective SVG `rect` element.
                    elementArray[i - 1].style.stroke = color;
                    elementArray[i - 1].style.strokeWidth = 3;
                    elementArray[i - 1].style.fill = "#ffffff";

                    // Show and hide actor tooltip on hover to text
                    elementArray[i].addEventListener("mouseenter", (event) => {
                        this.showTooltip(event, cellName);
                    });
                    elementArray[i].addEventListener("mouseleave", () => {
                        this.hideTooltip();
                    });
                    // Show and hide actor tooltip on hover to rectangle
                    elementArray[i - 1].addEventListener("mouseenter", (event) => {
                        this.showTooltip(event, cellName);
                    });
                    elementArray[i - 1].addEventListener("mouseleave", () => {
                        this.hideTooltip();
                    });

                    // Truncating cell name when cellName is too long
                    const letterLength = elementArray[i].getBBox().width / elementArray[i].textContent.length;
                    const lettersLength = Math.round(elementWidth / letterLength);
                    if (elementArray[i].getBBox().width > elementWidth) {
                        const truncatedCellName = `${cellName.substring(0, lettersLength - 5)}...`;
                        elementArray[i].textContent = truncatedCellName;
                    }
                }
            }
        }
    }

    /**
     * Show tooltip.
     *
     * @param {event} evt Mouse event
     * @param {string} text The actor name
     */
    showTooltip = (evt, text) => {
        const tooltip = document.getElementById("tooltip");
        tooltip.innerHTML = text;
        tooltip.style.display = "block";
        tooltip.style.left = `${evt.pageX}px`;
        tooltip.style.top = `${evt.pageY}px`;
    };

    /**
     * Hide tooltip.
     */
    hideTooltip = () => {
        const tooltip = document.getElementById("tooltip");
        tooltip.style.display = "none";
    };

    /**
     * Draw the Cell level Sequence Sequence.
     */
    drawCellLevelSequenceDiagram = () => {
        const {spans} = this.props;
        const tree = TracingUtils.getTreeRoot(spans);

        const resolveActorName = (span) => SequenceDiagram.sanitizeActorName(
            span.cell ? span.cell.name : SequenceDiagram.GLOBAL_GATEWAY);
        const resolveCallingId = (id) => id;

        this.setState({
            selectedCell: "",
            selectedActionId: ""
        });
        this.drawSequenceDiagram(tree, resolveActorName, resolveCallingId);
    };

    /**
     * Draw the Component level Sequence Sequence.
     *
     * @param {number} actionId The Id of the selected Cell Level action
     */
    drawComponentLevelSequenceDiagram = (actionId) => {
        const {spans} = this.props;
        const subTree = spans.find((span) => (span.actionId && span.actionId === actionId));

        const resolveActorName = (span) => SequenceDiagram.sanitizeActorName(span.serviceName);
        const resolveCallingId = (id) => `${actionId}.${id}`;
        const shouldTerminate = (span) => ((subTree.cell !== null && span.cell === null)
            || (subTree.cell === null && span.cell !== null)
            || subTree.cell.name !== span.cell.name);

        this.setState({
            selectedActionId: actionId
        });
        this.drawSequenceDiagram(subTree, resolveActorName, resolveCallingId, shouldTerminate);
    };

    /**
     * Draw a Sequence Diagram.
     * This updates the state with the Sequence diagram data.
     *
     * @param {Span} tree The tree for which the sequence diagram should be generated
     * @param {Function} resolveActorName Function to generate the actor name from span
     * @param {Function} resolveCallingId Function to generate the action Id
     * @param {Function} shouldTerminate Function for deciding whether the tree traversing should terminate
     */
    drawSequenceDiagram(tree, resolveActorName, resolveCallingId, shouldTerminate = null) {
        const actors = [];
        let initialSpan;
        const addActorIfNotPresent = (span) => {
            const actor = resolveActorName(span);
            if (!actors.includes(actor)) {
                actors.push(actor);
            }
            if (!initialSpan) {
                initialSpan = span;
            }
        };

        let actionId = 1;
        const actions = [];
        tree.walk((span, data) => {
            let linkSource = data;
            if (!Constants.System.SIDECAR_AUTH_FILTER_OPERATION_NAME_PATTERN.test(span.operationName)
                && !Constants.System.ISTIO_MIXER_NAME_PATTERN.test(span.serviceName)
                && (!linkSource || resolveActorName(linkSource) !== resolveActorName(span))) {
                const linkTarget = span;
                if (linkSource && linkTarget.kind === Constants.Span.Kind.SERVER) { // Ending link traversing
                    const linkSourceActor = resolveActorName(linkSource);
                    const linkTargetActor = resolveActorName(linkTarget);
                    actions.push(`${linkSourceActor}->>+${linkTargetActor}: `
                        + `Call ${linkTargetActor} [${resolveCallingId(actionId)}] \n`);
                    linkTarget.actionId = actionId;
                    actionId += 1;
                    linkSource = null;
                } else if (!linkSource && span.kind === Constants.Span.Kind.CLIENT) { // Starting link traversing
                    linkSource = span;
                }
                addActorIfNotPresent(span);
            }
            return linkSource;
        }, null, (span, data) => {
            const linkSource = span;
            const linkTarget = data;
            if (!Constants.System.SIDECAR_AUTH_FILTER_OPERATION_NAME_PATTERN.test(linkSource.operationName)
                && !Constants.System.ISTIO_MIXER_NAME_PATTERN.test(linkSource.serviceName)
                && (!linkTarget || resolveActorName(linkTarget) !== resolveActorName(linkSource))) {
                if (linkTarget && linkSource.kind === Constants.Span.Kind.SERVER) { // Ending link traversing
                    actions.push(`${resolveActorName(linkSource)}-->>-${resolveActorName(linkTarget)}: Return \n`);
                }
            }
        }, shouldTerminate);

        // Generating the sequence diagram data. This string is used by mermaid for generating the Sequence Diagram.
        let sequenceDiagramData = "sequenceDiagram\n";
        for (let i = 0; i < actors.length; i++) {
            sequenceDiagramData += `participant ${actors[i]}\n`;
        }
        if (initialSpan) {
            sequenceDiagramData += `activate ${resolveActorName(initialSpan)}\n`;
        }
        for (let i = 0; i < actions.length; i++) {
            sequenceDiagramData += actions[i];
        }
        if (initialSpan) {
            sequenceDiagramData += `deactivate ${resolveActorName(initialSpan)}\n`;
        }
        this.setState({
            sequenceDiagramData: sequenceDiagramData
        });
    }

    /**
     * Sanitize the actor name for mermaid.
     * Dashes are not supported mermaid in the actor names. Therefore this is replaced with an underscore.
     *
     * @param {string} name The actor name that needs to be sanitized
     * @returns {string} name The sanitized actor name
     */
    static sanitizeActorName(name) {
        return name.replace(/-/g, "_");
    }

    /**
     * Get the original actor name from the sanitized name.
     * This is used when the name is identified from the HTML elements created by mermaid.
     * The original actor name cannot contain "_"s. Therefore this replacement would guarantee
     * getting back the original actor name.
     *
     * @param {string} name The already sanitized actor name
     * @returns {string} The actual actor name
     */
    static getActorIdFromSanitizedName(name) {
        return name.replace(/_/g, "-");
    }

}

SequenceDiagram.propTypes = {
    classes: PropTypes.object.isRequired,
    spans: PropTypes.arrayOf(
        PropTypes.instanceOf(Span).isRequired
    ).isRequired,
    colorGenerator: PropTypes.instanceOf(ColorGenerator)
};

export default withStyles(styles, {withTheme: true})(withColor(SequenceDiagram));
