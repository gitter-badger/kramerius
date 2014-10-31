
/**
 * Register listener -> Create viewer 
 */
var delayedEvent = {'pid':'','enabled':true};        
K5.eventsHandler.addHandler(function(type, configuration) {
    var splitted = type.split("/");
    if (splitted.length == 3) {
        //api/item/
        if ((splitted[0] === "api") && (splitted[1] === "item")) {
            var pid = splitted[2];
            delayedEvent.pid=pid;    
            if (K5.initialized) {
                _eventProcess(pid);
                delayedEvent.enabled = false;
            } else {
                delayedEvent.enabled = true;
            }
        }
    }
    if (type === "application/init/end") {
        if ((delayedEvent.enabled)  && (K5.api.ctx["item"] && K5.api.ctx["item"][delayedEvent.pid])) {
                _eventProcess(delayedEvent.pid);
        }
    }
    if (type === "widow/url/hash") {
        var pid = location.hash;
        if (K5.api.ctx.item && K5.api.ctx.item[pid.substring(1)]) {
            if (K5.api.ctx.item[pid.substring(1)].pid) {
                    var data = K5.api.ctx.item[pid.substring(1)];
                    K5.eventsHandler.trigger("api/item/" + pid.substring(1), data);
            } else {
                    K5.api.askForItem(pid.substring(1));
            }       
        } else {
            K5.api.askForItem(pid.substring(1));
        }
    }

    // changes in context buttons
    if (type === "application/menu/ctxchanged") {
        K5.gui["selected"].addContextButtons();
    }
});

var pid = location.hash;
if (pid) K5.api.askForItem(pid.substring(1));

var maxwidth = $('html').css('max-width');

var w = (window.innerWidth > 0) ? window.innerWidth : screen.width;
K5.serverLog("window width :"+w);

function _eventProcess(pid) {

    var data = K5.api.ctx["item"][pid];
    var viewer = K5.gui["viewers"].select(data);

    K5.api.ctx["item"]["selected"] = pid;
    if (K5.gui.selected) {
        K5.gui.selected.clearContainer();
    }

    var okfunc = _.bind(function() {
        
        var instance = K5.gui["viewers"].instantiate(viewer.object);        

        K5.gui["selected"] = mixInto(new ItemSupport(K5), instance);
        K5.gui["selected"].initItemSupport();
        K5.gui["selected"].open();
        K5.gui["selected"].ctxMenu();    

        _metadatainit();


        K5.gui.selected["disabledDisplay"] = false;
    });
    var failfunc = _.bind(function() {
        var nviewer = K5.gui["viewers"].findByName('forbidden');
        var instance = K5.gui["viewers"].instantiate(nviewer.object);        

        K5.api.ctx["item"][pid]['forbidden'] = true;

        K5.gui["selected"] = mixInto(new ItemSupport(K5), instance);
        K5.gui["selected"].initItemSupport();
        K5.gui["selected"].open();

        // initialization
        K5.gui["selected"].ctxMenu();    
        _metadatainit();
        
        K5.gui.selected["disabledDisplay"] = false;
    });

    K5.gui["viewers"].forbiddenCheck(viewer.object,okfunc,failfunc); 
        

    function _metadatainit() {
            // metadata initialization 
            $("#metadata").hide();
            if (data.model === "page") {
                $("#model").show();
                $("#title").show();
                $("#root_title").hide();
            } else {
                $.get("metadata?pid=" + pid + "&model=" + K5.api.ctx.item[pid].model, _.bind(function(data) {
                    $("#model").hide();
                    $("#title").hide();
                    $("#root_title").hide();
                    $("#metadata").html(data);
                    $(".infobox .label").each(function(index, val) {
                        var txt = $(val).text();
                        txt = txt.trim()
                        if (txt.indexOf(":") === 0) {
                            $(val).text('');
                        }
                    });
                   $(".infobox .label").each(function(index, val) {
                        var valueText = $(val).siblings(".value").text();
                        valueText = valueText.trim()
                        if ("" === valueText) {
                            $(val).siblings(".value").remove();
                            $(val).remove();
                        }
                    });
                    $("#metadata").show();
                }, this));
            }
    }    
        
}


/**
 * Basic item support. <br> Instance is mixed with concrete implementation ( {@link Zoomify},{@link ZoomifyStaticImage})  and it is accessible via property K5.gui.selected <br>
 * @constructor
 * @param {Application} application - The application instance  {@link Application}.
 */
function ItemSupport(application) {
    this.application = application;
}


ItemSupport.prototype = {
    initItemSupport: function() {
        if (this.application.i18n.ctx && this.application.i18n.ctx.dictionary) {
            this._initInfo();
        } else {
            this.application.eventsHandler.addHandler(_.bind(function(type, configuration) {
                console.log("event type " + type);
                if (type == "i18n/dictionary") {
                    this._initInfo();
                }
            }, this));
        }
    },

    _initInfo: function() {


        var pid = K5.api.ctx["item"]["selected"];
        var root_title = K5.api.ctx["item"][pid].root_title;
        $(document).prop('title', K5.i18n.ctx.dictionary['application.title'] + ". " + root_title);


        this.renderContext();
        /*this.addContextButtons();*/

        /*if (($.cookie('item_showinfo') === 'undefined') || $.cookie('item_showinfo') === 'true') {
            this.showInfo();
        }*/
    },

    /**
     * Explore data-ctx attribute and add header button
     * allowed values are all , item , selected , notselected
     * @method 
     */
    addContextButtons: function() {
        $("#contextbuttons").html("");
        $("#item_menu>div").each(function() {
            if ($(this).data("ctx")) {
                var a = $(this).data("ctx").split(";");
                // all context
                if (jQuery.inArray('all', a) > -1) {
                    $("#contextbuttons").append($(this).clone());
                }
                // only selected
                if (jQuery.inArray('selected', a) > -1) {
                    if (K5.gui.clipboard.isCurrentSelected()) {
                        $("#contextbuttons").append($(this).clone());
                    }
                }

                // only notselected
                if (jQuery.inArray('notselected', a) > -1) {
                    if (!K5.gui.clipboard.isCurrentSelected()) {
                        $("#contextbuttons").append($(this).clone());
                    }
                }

                // only clipboard
                if (jQuery.inArray('clipboardnotempty', a) > -1) {
                    if (K5.gui.clipboard.getSelected().length > 0) {
                        $("#contextbuttons").append($(this).clone());
                    }
                }

                // next context
                if (jQuery.inArray('next', a) > -1) {
                        if (K5.api.ctx["item"][selected]["siblings"]) {
                                var data = K5.api.ctx["item"][selected]["siblings"];
                                var arr = data[0]['siblings'];
                                var index = _.reduce(arr, function(memo, value, index) {
                                        return (value.selected) ? index : memo;
                                }, -1);
                                if (index<arr.length-1) { 
                                        $("#contextbuttons").append($(this).clone());
                                }  
                        }
                }

                // prev context
                if (jQuery.inArray('prev', a) > -1) {
                        if (K5.api.ctx["item"][selected]["siblings"]) {
                                var data = K5.api.ctx["item"][selected]["siblings"];
                                var arr = data[0]['siblings'];
                                var index = _.reduce(arr, function(memo, value, index) {
                                        return (value.selected) ? index : memo;
                                }, -1);
                                if (index>0) { 
                                        $("#contextbuttons").append($(this).clone());
                                }  
                        }
                }
            }
        });
    },

   /**
    * Render ctx menu 
    * @method
    */     
   ctxMenu: function() {
        $("#acts_container").empty();

        var menuDiv = $("<div/>", {'id': 'ctxmenu'});
        var ul = $('<ul/>');
        var items = _.map(K5.gui.nmenu.ctx.actions, function(a) {
                var li = $('<li/>', {'id': 'ctxmenu-'+a.name});
                var item = $('<a/>', {'href': 'javascript:K5.gui.nmenu.action("' + a.name+'")', 'data-key': a.i18nkey});
                item.addClass("translate");
                li.append(item);
                return li;
        });

        _.each(items, function(itm) {
            if (itm != null) ul.append(itm);
        });
        menuDiv.append(ul);
        $("#acts_container").append(menuDiv);
        if (K5.i18n.ctx.dictionary) {
                K5.i18n.k5translate(menuDiv);
        }
    },
 
   _toOld: function(actions) {
        var menuDiv = $("<div/>", {'id': 'ctxmenu'});
        var ul = $('<ul/>');
        var items = _.map(actions, function(a) {
            if (a.visible) {
                var li = $('<li/>');
                var item = $('<a/>', {'href': 'javascript:' + a.action, 'data-key': a.i18nkey});
                item.addClass("translate");
                li.append(item);
                return li;
            } else
                return null;
        });

        _.each(items, function(itm) {
            if (itm != null)
                ul.append(itm);
        });

        menuDiv.append(ul);
        $("#acts_container").append(menuDiv);

        K5.i18n.k5translate(menuDiv);
    },



    renderContext: function() {
        $(".context").remove();
        var pid = K5.api.ctx["item"]["selected"];
        var data = K5.api.ctx["item"][pid];

        // update model
        var model = data.model;

        K5.i18n.translatableElm("fedora.model." + K5.api.ctx.item[pid].model, "#model");
        $("#title").text(K5.api.ctx.item[pid].title);
        if (data.model === "page") {
            if (data.details && data.details.type) {
                if (data.details.type !== "normalPage" && data.details.type !== "NormalPage") {
                    var type = $(K5.i18n.translatable("mods.page.partType." + data.details.type));
                    type.addClass("pageType");
                    var title = $("<span/>");
                    title.text(K5.api.ctx.item[pid].title + " ");

                    $("#title").empty();

                    $("#title").append(title);
                    $("#title").append(type);
                } else {
                    $("#title").html("<span>" + K5.api.ctx.item[pid].title + "</span>");
                }
            } else {
                $("#title").html("<span>" + K5.api.ctx.item[pid].title + "</span>");
            }
        } else {
            $("#title").html("<span>" + K5.api.ctx.item[pid].title + "</span>");
        }


        this.itemContext = data.context[0];
        var contextDiv = $("<div/>", {class: "context"});
        for (var i = 0; i < this.itemContext.length - 1; i++) {
            var p = this.itemContext[i].pid;
            var div = $('<div/>');
            $(div).css("margin-left", (i * 15) + "px");
            var a = $('<div/>', {'data-pid': p});
            var img = $('<img/>', {'src': 'api/item/' + p + '/thumb'});
            img.css('height', '48px');
            div.append(img);
            var model = K5.i18n.translatable('fedora.model.' + this.itemContext[i].model);

            a.data('pid', p);

            if (K5.api.ctx.item[p]) {
                a.append('<span> ' + model + '</span>');
                a.append('<span> (' + K5.api.ctx.item[p].title + ')</span>');
            } else {
                K5.api.askForItemContextData(p, function(data) {
                    var pidElm = $("div:data(pid)").filter(function() {
                        return $(this).data("pid") === data.pid;
                    });
                    var m = K5.i18n.translatable('fedora.model.' + data.model);
                    pidElm.append('<span> ' + m + '</span>');
                    pidElm.append('<span> (' + data.title + ')</span>');
                });
            }

            a.click(_.bind(function(l) {
                K5.api.gotoItemPage(l, $("#q").val());
            }, this, p));

            div.append(a);
            contextDiv.append(div);
        }
        contextDiv.insertBefore(".mtd_footer");
    },

    hidePages: function() {
        $("#itemparts").hide();
    },
    showItemNavigation: function() {
        this.hideInfo();
    },

    // toggle actions -> change it     
    toggleActions: function() {
        /*
        if ($('#ctxmenu').size() == 0) {
            console.log("javascript request");
            $.getScript("js/menu/menuload.js", _.bind(function(data, textStatus, jqxhr) {
                this._initCtxMenu(K5.gui.menu.actions);
            }, this)).fail(function(jqxhr, settings, exception) {
            });
        } else {
            $('#ctxmenu').toggle();
        }*/
    },
    /**
     * Siblings request
     * @method
     */    
    siblings: function() {
        $("#itemparts").append("<div id='itempartssiblings' style='overflow:scroll; width:100%; height:40%; text-align:center'><h1>Siblings</h1></div>");
        K5.api.askForItemSiblings(K5.api.ctx["item"]["selected"], function(data) {
            console.log(data.length);
            var arr = data[0]['siblings'];
            console.log('array length:' + arr);
            var str = _.reduce(arr, function(memo, value, index) {
                var pid = value.pid;
                memo +=
                        "<div style='float:left'> <a href='?page=doc&pid=" + pid + "'> <img src='api/item/" + pid + "/thumb'/></a> </div>";
                return memo;
            }, "");
            $("#itempartssiblings").append(str + "<div style='clear:both'></div>");

        });

    },
    /**
     * Children request
     * @method
     */
    children: function() {
        $("#itemparts").append("<div id='itempartschildren' style='overflow:scroll; width:100%; height:40%;text-align:center'><h1>Children</h1></div>");
        K5.api.askForItemChildren(K5.api.ctx["item"]["selected"], _.bind(function(data) {
            console.log("received data");
            var arr = data;
            var str = _.reduce(data, function(memo, value, index) {
                var pid = value.pid;
                memo +=
                        "<div style='float:left'> <a href='?page=doc&pid=" + pid + "'> <img src='api/item/" + pid + "/thumb'/></a> </div>";
                return memo;
            }, "");
            $("#itempartschildren").html(str + "<div style='clear:both'></div>");
        }, this));
    },
    /**
     * Returns true if the current item has parent
     * @method      
     */    
    hasParent: function() {
        return (this.itemContext.length > 1);
    },
    /**
     * Returns parent pid
     * @method      
     */       
    parent: function() {
        cleanWindow();
        
        if (this.itemContext.length > 1) {
            var parentPid = this.itemContext[this.itemContext.length - 2].pid;
            K5.api.gotoItemPage(parentPid, $("#q").val());
        }
    },
    /**
     * Next item
     * @method      
     */
    next: function() {

        cleanWindow();

        if (K5.api.isKeyReady("item/selected") && (K5.api.isKeyReady("item/" + K5.api.ctx.item.selected + "/siblings"))) {
            var data = K5.api.ctx["item"][ K5.api.ctx["item"]["selected"] ]["siblings"];
            var arr = data[0]['siblings'];
            var index = _.reduce(arr, function(memo, value, index) {
                return (value.selected) ? index : memo;
            }, -1);
            if (index <= arr.length - 2) {
                var nextPid = arr[index + 1].pid;
                K5.api.gotoItemPage(nextPid, $("#q").val());
            }
        } else {
            K5.api.askForItemSiblings(K5.api.ctx["item"]["selected"], function(data) {
                var arr = data[0]['siblings'];
                var index = _.reduce(arr, function(memo, value, index) {
                    return (value.selected) ? index : memo;
                }, -1);
                if (index < arr.length - 2) {
                    var nextPid = arr[index + 1].pid;
                    K5.api.gotoItemPage(nextPid, $("#q").val());
                }
            });
        }
    },
    /**
     * Previous item
     * @method      
     */       
    prev: function() {

        cleanWindow();

        //this.clearContainer();
        if (K5.api.isKeyReady("item/selected") && (K5.api.isKeyReady("item/" + K5.api.ctx.item.selected + "/siblings"))) {
            var data = K5.api.ctx["item"][ K5.api.ctx["item"]["selected"] ]["siblings"];
            var arr = data[0]['siblings'];
            var index = _.reduce(arr, function(memo, value, index) {
                return (value.selected) ? index : memo;
            }, -1);
            if (index > 0) {
                var prevPid = arr[index - 1].pid;
                K5.api.gotoItemPage(prevPid, $("#q").val());
            }

        } else {
            K5.api.askForItemSiblings(K5.api.ctx["item"]["selected"], function(data) {
                var arr = data[0]['siblings'];
                var index = _.reduce(arr, function(memo, value, index) {
                    return (value.selected) ? index : memo;
                }, -1);
                if (index > 0) {
                    var prevPid = arr[index - 1].pid;
                    K5.api.gotoItemPage(prevPid, $("#q").val());
                }
            });
        }
    },
        
    /**
     * Toggle info panel
     * @method      
     */       
    togglePin: function() {
        $("#viewer>div.info").toggleClass("pin");
    },

    /**
     * Hide info panel
     * @method      
     */       
    hideInfo: function() {
        this.hidePanel("#viewer>div.info", 290, -500, 200);
        $.cookie('item_showinfo', "false");
    },

    /**
     * Search inside document
     * @method      
     * @param {integer} speed.
     */       
    searchInside: function(speed) {
        cleanWindow();

        $("#searchinside_q").val($("#q").val());

        divopen("#viewer>div.searchinside");
        $("#searchinside_q").focus();
        $("#searchinside_q").select();

        this._searchInsideArrow();

//        this.showPanel("#viewer>div.searchinside", 290, 37, speed);
        /*
        this.hidePanels(_.bind(function(){
            this.showPanel("#viewer>div.searchinside", 290, 37, speed);
            $("#searchinside_q").focus();
            $("#searchinside_q").select();
        }, this));
        */

    },

    /**
     * Show info panel
     * @method      
     */       
    showInfo: function(speed) {
        cleanWindow();
        divopen("#viewer>div.info");

        var metadataheight = $("#metadata").height();
        console.log("metadata height :"+metadataheight);

        var contextheight = $(".context").height();
        console.log("context height :"+contextheight);

        var titleheight = $("#title").height()
        var modelheight = $("#model").height()

        var nheight = metadataheight + 63 +contextheight+titleheight+modelheight ;

        $("#viewer .infobox").height(nheight);

        function triangle(nheight) {
                var v = $("#ctxmenu").height();     
                $("#viewer .actions").css("height",v + 55 +"px"); 
                var viewerWidth = $("#viewer").width();
                var offset = $("#vwr_ctx_metadata").offset();
                var actionsWidth = $("#vwr_ctx_metadata").width();
                var headerHeight = $("#header").height();
                var toffset = $("#mtd_footer_triangle").offset();
                var tw = $("#mtd_footer_triangle").width();
                var left = offset.left - (8) +(actionsWidth/2);
                var toff = {
                        "top":toffset.top,
                        "left":left
                };

                $("#mtd_footer_triangle").offset({"top":toff.top, "left":toff.left});                        
                return toff;
        } 

        triangle(nheight);
    },

    toggleHits: function() {
        $("li.ishit").toggleClass('hit');
        $("li.containhit").toggleClass('chit');
    },

    
    hidePanels: function(whenready) {
        if($("#viewer>div.infobox:visible").length===0){
            if (whenready) whenready.apply(null);
        }else{
            this.hidePanel("#viewer>div.infobox:visible", 290, -500, 200, whenready);
        }
    },


    /** 
     * toggle actions 
     * @method
     */
    toggleActions: function() {
        if (visible("#viewer>div.actions")) { cleanWindow(); } 
        else { showActions();  }
    },

    _searchInsideArrow:function() {

        var offset = $("#vwr_ctx_searchinside").offset();
        var w = $("#vwr_ctx_searchinside").width();
        var toffset = $("#searchinside_triangle");
        var tw = $("#searchinside_triangle").width();
        var left = offset.left - (8) +(w/2);

        $("#searchinside_triangle").offset({"top":toffset.top, "left":left});                        

    },
   
    _actionsArrow:function() {
        function triangle() {
                var v = $("#ctxmenu").height();     
                
                $("#viewer .actions").css("height",v + 55 +"px"); 

                var offset = $("#vwr_ctx_actions").offset();
                var actionsWidth = $("#vwr_ctx_actions").width();


                var toffset = $("#acts_footer_triangle").offset();
                var tw = $("#acts_footer_triangle").width();
                var left = offset.left - (8) +(actionsWidth/2);

                var toff = {
                        "top":toffset.top,
                        "left":left
                };

                $("#acts_footer_triangle").offset({"top":toff.top, "left":toff.left});                        
                return toff;
        } 
        triangle();
    },    
    
    
    /**
     * Show menu actions
     * @method      
     */       
    showActions: function() {
        cleanWindow();
        divopen("#viewer>div.actions");
        K5.gui.nmenu.refreshActions();
        this._actionsArrow();
    },

    hidePanel: function(panel, l, t, speed, whenready) {
        $(panel).animate({'opacity': '0.5', 'left': l, 'top': t}, speed, function() {
            $(panel).removeClass("showing");
            $(panel).hide();
            if (whenready) whenready.apply(null);
        });
    },

        
        
    showPanel: function(panel, l, t, speed) {
        if (!$(panel).hasClass("showing")) {
            $(panel).addClass("showing");
            $(panel).show();

            $(panel).animate({'opacity': '1.0', 'left': l, 'top': t}, speed);

            $(panel).animate({'opacity': '1.0', 'left': l, 'top': t}, speed);
        }
    }
};





