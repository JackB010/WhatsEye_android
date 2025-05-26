package com.example.whatseye.dataType

class JavaScriptCode   {
    companion object {

        val CONTACT = """(async ()=>{
           ng = document.querySelector('[data-icon="new-chat-outline"]')
           ng.click()
           await new Promise(resolve => setTimeout(resolve, 10));
           const contactContainer = document.querySelector("div[data-tab='4']");
           const parentElement = contactContainer.parentElement;    
        
           if (contactContainer.querySelectorAll('[role="listitem"]').length === 0) return []
           contactContainer.querySelectorAll('[role="listitem"]').forEach(item=>{ 
              if(item.querySelector("span[title]"))
                 item.setAttribute('aria-selected_0',"false") 
           })
           
           const arrowDownEvent = new KeyboardEvent('keydown', {
                    key: 'ArrowDown',
                    code: 'ArrowDown',
                    keyCode: 40,
                    which: 40,
                    bubbles: true
                });
           const searchBox = document.querySelector('[role="textbox"]');
           searchBox.focus();
           searchBox.dispatchEvent(arrowDownEvent);
           
           nb = contactContainer.firstChild.firstChild.style.height.replace('px', '')/72
           contacts = []
        
           for(let i=0; i<nb; i++){
              await new Promise(resolve => setTimeout(resolve, 120));
              item = contactContainer.querySelector('[aria-selected_0="false"]')
              if(item.getAttribute('DONE') == null){
                 item.setAttribute('DONE', 'true')
                 let name = item.querySelector("span[title]").innerText;   
                 let about = item.querySelector("div[role='gridcell']").parentElement.childNodes[1].firstChild.innerText
                 let icon = item.querySelector('img') ? item.querySelector('img').src : "";
                 contacts.push({ name, about, icon });
                 //contacts.push({ name});
              }
              item.dispatchEvent(arrowDownEvent);
              item.removeAttribute('aria-selected_0')
        
              if(contactContainer.querySelectorAll('[aria-selected_0="false"]').length ===0){
                 if( contactContainer.querySelectorAll('[role="listitem"]').length !== 0){
                     contactContainer.querySelectorAll('[role="listitem"]').forEach(item=>{ 
                    if(item.querySelector("span[title]"))
                       item.setAttribute('aria-selected_0',"false") 
                    })
                 }else{
                    return contacts 
                 }
              }
           }
              bc = document.querySelector('[data-icon="back"]')
              bc.click()
              localStorage.setItem("CONTACT", JSON.stringify(contacts));
            })();""".trimIndent()
        val CURRENT_CHATS = """(async function () {
                    function delay(ms) {
                        return new Promise(function (resolve) {
                            setTimeout(resolve, ms);
                        });
                    }
                    
                    var listItems = document.querySelectorAll('[role="listitem"]');
                    if (listItems.length === 0) return [];
                    
                    var arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
                    
                    var searchBox = document.querySelector('[role="textbox"]');
                    searchBox.focus();
                    searchBox.dispatchEvent(arrowDownEvent);
                    
                    var rowCount = parseInt(document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount'), 10);
                    var contacts = [];
                    
                    for (var i = 0; i < rowCount; i++) {
                        await delay(10*i);
                    
                        var selectedItems = document.querySelectorAll('[aria-selected="true"]');
                        var item = selectedItems[1];
                        if (!item) continue;
                    
                        try {
                            var nameElem = item.querySelector("span[title]");
                            if (!nameElem) continue;
                    
                            var name = nameElem.innerText;
                            var gridCell = item.querySelector("div[role='gridcell']");
                            var timestamp = gridCell && gridCell.lastChild ? gridCell.lastChild.innerText : "";
                            var lastMsg = "";
                            try {
                                lastMsg = gridCell.parentElement.childNodes[1].querySelector('[title]').innerText;
                            } catch (e) {
                                lastMsg = "";
                            }
                            var unreadCell = item.querySelector('[role="gridcell"][aria-colindex="1"]');
                            var numUnread = unreadCell && unreadCell.firstChild ? unreadCell.firstChild.innerText : "";
                            var iconImg = item.querySelector('img');
                            var icon = iconImg ? iconImg.src : "";
                    
                            contacts.push({
                                name: name,
                                timestamp: timestamp,
                                num_unread: numUnread,
                                last_msg: lastMsg,
                                icon: icon
                            });
                        } catch (err) {
                            console.warn("Error extracting contact:", err);
                        }
                    
                        item.dispatchEvent(arrowDownEvent);
                    }
                    
                    var lang = document.querySelector('html').getAttribute('lang') || 'en';
                    var unreadLabels = {
                        en: "Unread",
                        ar: "غير مقروءة",
                        fr: "Non lues"
                    };
                    
                    var unreadLabel = unreadLabels[lang];
                    var item = document.querySelectorAll('[aria-selected="true"]')[1];
                    
                    if (item && item.querySelector('[aria-label="' + unreadLabel + '"]')) {
                        await delay(300);
                        var button = item.querySelector('button');
                        if (button) button.click();
                        await delay(300);
                    
                        var markAsUnreadLabels = {
                            en: "Mark as unread",
                            ar: "تمييز كغير مقروءة",
                            fr: "Marquer comme non lue"
                        };
                    
                        var markAction = document.querySelector('[aria-label="' + markAsUnreadLabels[lang] + '"]');
                        if (markAction) markAction.click();
                    }
                    
                    await delay(300);
                    
                    var menuButtons = document.querySelectorAll('[data-icon="menu"]');
                    if (menuButtons[1]) {
                        menuButtons[1].click();
                    }
                    
                    await delay(300);
                    
                    var closeChatLabels = {
                        en: "Close chat",
                        ar: "إغلاق الدردشة",
                        fr: "Fermer la discussion"
                    };
                    
                    var closeAction = document.querySelector('[aria-label="' + closeChatLabels[lang] + '"]');
                    if (closeAction) closeAction.click();
                    
                    await delay(300);
                    
                    var paneSide = document.getElementById("pane-side");
                    if (paneSide) {
                        paneSide.scrollTop = 0;
                    }
                    
                    localStorage.setItem("CURRENT_CHATS", JSON.stringify(contacts));
                    })();
                    """.trimIndent()
        val BLOCK_USER = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('[role="textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount') 
               let item_pos=0;
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name ) {
                     name = name.innerText;
                     if (search === name){
                        if (item_pos===pos ) {
                            await new Promise(resolve => setTimeout(resolve, 300));
                            item.querySelector('button').click()
                            await new Promise(resolve => setTimeout(resolve, 300));
                            const lang = document.querySelector('html').getAttribute('lang')
                            if (lang=='en')
                                action = document.querySelector('[aria-label="Block"]')
                            if (lang=='fr')
                                action = document.querySelector('[aria-label="Bloquer"]')
                            if (lang=='ar')
                                action = document.querySelector('[aria-label="حظر"]')
                            
                            let block = true

                            if(!action){
                                if (lang=='en')
                                    action = document.querySelector('[aria-label="Exit group"]')
                                if (lang=='fr')
                                    action = document.querySelector('[aria-label="Quitter le groupe"]')
                                if (lang=='ar')
                                    action = document.querySelector('[aria-label="الخروج من المجموعة"]')
                                    
                                block = false
                            }
                            if(action){
                                action.click()
                                await new Promise(resolve => setTimeout(resolve, 200));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                            }

                                const labels_g = {
                                    en: 'Delete group',
                                    fr: 'Supprimer le groupe',
                                    ar: 'حذف المجموعة'
                                    }
                                 var labels_c = {
                                    en:'Delete chat',
                                    fr:'Supprimer la discussion',
                                    ar:'حذف الدردشة'
                                    };
                                label_c = labels_c[lang];
                                label_g = labels_g[lang];
                                item.querySelector('button').click();
                                await new Promise(resolve => setTimeout(resolve, 100));
                                //if (lang=='en')
                                if (document.querySelector('[aria-label="' + label_c + '"]'))
                                    document.querySelector('[aria-label="' + label_c + '"]').click()
                                else
                                     document.querySelector('[aria-label="' + label_g + '"]').click()

                                await new Promise(resolve => setTimeout(resolve, 100));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                         
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
            })""".trimIndent()



        val SELECT_ROOM = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('[role="textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-rowcount]').getAttribute('aria-rowcount') 
               let item_pos=0;
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name ) {
                     name = name.innerText;
                     if (search === name){
                     
                        if (item_pos===pos ) {
                        await new Promise(resolve => setTimeout(resolve, 600));       
                        if(item.querySelector('[aria-label="Unread"]') || item.querySelector('[aria-label="غير مقروءة"]') || item.querySelector('[aria-label="Non lues"]') ){
                            await new Promise(resolve => setTimeout(resolve,    300));
                            item.querySelector('button').click()
                            await new Promise(resolve => setTimeout(resolve,    300));
                            const lang = document.querySelector('html').getAttribute('lang')
                            
                            if (lang=='en'){
                               action =  document.querySelector('[aria-label="Mark as unread"]')
                               if (action)
                                    action.click()
                               }
                            if (lang=='ar'){
                                 action =document.querySelector('[aria-label="تمييز كغير مقروءة"]')
                                 if (action)
                                    action.click()
                                }
                            if (lang=='fr'){
                                 action =document.querySelector('[aria-label="Marquer comme non lue"]')
                                 if (action)
                                    action.click()
                                }
                        }   
                        await new Promise(resolve => setTimeout(resolve, 300));
                        document.getElementById("pane-side").scrollTop = 0;
                        await new Promise(resolve => setTimeout(resolve,    300));
                        await localStorage.setItem("ROOM_SELECTED", true);
                        await new Promise(resolve => setTimeout(resolve,    300));
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
                
            })""".trimIndent()

        val CHAT_ROOM_SCROLLER2 = """(async () => {
            await new Promise(resolve => setTimeout(resolve,    600));
                const root = document.querySelector('[id="main"]');
                const room = root.querySelector('[role="application"]');
                let prev_scroll = 0;
                let timedOut = false;
               
                // Set timeout to mark as timed out after 30 seconds
            
                 
                 
               do {
               numrows = room.querySelectorAll('div[role="row"]').length 
               
                reload_a= document.querySelector('[title="Load earlier messages…"]')
                if(reload_a){
                await new Promise(resolve => setTimeout(resolve,    300));
                    reload_a.click()
                    await new Promise(resolve => setTimeout(resolve,    300));
                    reload_a.click()
                    }
                    prev_scroll = room.parentElement.scrollTop;
                    await new Promise(resolve => setTimeout(resolve, 5));
                    room.parentElement.scrollTop -= 80;
                    await new Promise(resolve => setTimeout(resolve, 15));
                    room.parentElement.scrollTop += 10;
                    await new Promise(resolve => setTimeout(resolve, 15));
                    messageRows = room.querySelectorAll('div[role="row"]').length
                }  while (prev_scroll !== room.parentElement.scrollTop)
            
                // Cleanup the timeout and set localStorage
                await new Promise(resolve => setTimeout(resolve,    100));
                localStorage.setItem("DONE_LOADING_CHAT2", true);
            })();
        """.trimIndent()

        val CHAT_ROOM_SCROLLER = """(async () => {
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    const root = document.querySelector('[id="main"]');
    const room = root?.querySelector('[role="application"]');

    if (!room) {
        console.error("Chat room not found.");
        return;
    }
          room.parentElement.click()
            await delay(200);
        
            let counted = 1;
            let messageRows = room.querySelectorAll('div[role="row"]');
        
            const arrowUpEvent = new KeyboardEvent('keydown', {
                key: 'ArrowUp',
                code: 'ArrowUp',
                keyCode: 38,
                which: 38,
                bubbles: true
            });
       
            while (counted <= messageRows.length) {
                const loadEarlierButton = document.querySelector('[title="Load earlier messages…"]');
                if (loadEarlierButton) {
                    await delay(200);
                    loadEarlierButton.click();
                    await delay(200);
                    loadEarlierButton.click(); // Maybe redundant — double-clicking
                }
        
                messageRows = room.querySelectorAll('div[role="row"]');
                const targetRow = messageRows[messageRows.length - counted];
        
                if (targetRow?.firstChild) {
                    await delay(30);
                    targetRow.firstChild.dispatchEvent(arrowUpEvent);
                    await delay(30);
                }
                 console.log(counted +"/ "+messageRows.length+"");
                counted++;
            }
            await delay(300);
            localStorage.setItem("DONE_LOADING_CHAT", true);
  
            console.log("Finished loading all messages.");
        })();
""".trimIndent()
        val GET_CHAT = """(async()=> {
                        await new Promise(resolve => setTimeout(resolve, 1000));
                        const root = document.querySelector('[id="main"]');
                        const room = root.querySelector('[role="application"]');
                        const messageList = []; 
                        const messageRows = room.querySelectorAll('div[role="row"]'); // Select all rows containing messages.
                    
                        await new Promise(resolve => setTimeout(resolve, 1000));
                        localStorage.setItem("CHATS", messageRows.length);
                        //return JSON.stringify(messageList, null, 2);
                    })();""".trimIndent()


    }
}