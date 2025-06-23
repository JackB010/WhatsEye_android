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
            await new Promise(resolve => setTimeout(resolve, 140));
           for(let i=0; i<nb; i++){
              await new Promise(resolve => setTimeout(resolve, 130));
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
                            localStorage.setItem("CONTACT_BLOCKED", true);
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
            })""".trimIndent()

        val BLOCK_USER_2 = """(async (search, pos=0)=>{
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                   await new Promise(resolve => setTimeout(resolve, 300));
                   item.querySelector('button').click()
                    await new Promise(resolve => setTimeout(resolve, 300));
                    const lang = document.querySelector('html').getAttribute('lang')
                    await new Promise(resolve => setTimeout(resolve, 200));

                    const labels_g = {
                        en: 'Delete group',
                        fr: 'Supprimer le groupe',
                        ar: 'حذف المجموعة'
                        }
                    const labels_c = {
                        en:'Delete chat',
                        fr:'Supprimer la discussion',
                        ar:'حذف الدردشة'
                        };
                    label_c = labels_c[lang];
                    label_g = labels_g[lang];
                    await new Promise(resolve => setTimeout(resolve, 300));
                    if (document.querySelector('[aria-label="' + label_c + '"]')){
                        await new Promise(resolve => setTimeout(resolve, 200));
                        document.querySelector('[aria-label="' + label_c + '"]').click()
                        }
                    else{
                        await new Promise(resolve => setTimeout(resolve, 200));
                         document.querySelector('[aria-label="' + label_g + '"]').click()
                         }
                    await new Promise(resolve => setTimeout(resolve, 200));
                    action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                    action[action.length -1].click()
                            localStorage.setItem("CONTACT_BLOCKED_2", true);
                            return;
                      
            })""".trimIndent()


        val SELECT_ROOM = """(async (search, pos = 0) => {
        try {
            const listItems = document.querySelectorAll('[role="listitem"]');
            if (listItems.length === 0) return [];

            const arrowDownEvent = new KeyboardEvent('keydown', {
                key: 'ArrowDown',
                code: 'ArrowDown',
                keyCode: 40,
                which: 40,
                bubbles: true
            });
            const searchBox = document.querySelector('[role="textbox"]');
            if (!searchBox) return;

            searchBox.focus();
            searchBox.dispatchEvent(arrowDownEvent);

            const rowCountElement = document.querySelector('[aria-rowcount]');
            if (!rowCountElement) return;
            const nb = parseInt(rowCountElement.getAttribute('aria-rowcount'), 10) || 0;

            let item_pos = 0;
            for (let i = 0; i < nb && i < 100; i++) { // Added max iteration limit
                await new Promise(resolve => setTimeout(resolve, 10));
                const selectedItems = document.querySelectorAll('[aria-selected="true"]');
                const item = selectedItems.length > 1 ? selectedItems[1] : selectedItems[0];
                if (!item) continue;

                const nameElement = item.querySelector('span[title]');
                if (!nameElement) continue;
                const name = nameElement.innerText;

                if (name === search) {
                    if (item_pos === pos) {
                        await new Promise(resolve => setTimeout(resolve, 100));
                        const unreadLabels = ['[aria-label="Unread"]', '[aria-label="غير مقروءة"]', '[aria-label="Non lues"]'];
                        if (unreadLabels.some(label => item.querySelector(label))) {
                            const button = item.querySelector('button');
                             await new Promise(resolve => setTimeout(resolve, 100));
                            if (button) button.click();

                            await new Promise(resolve => setTimeout(resolve, 300));
                            const lang = document.querySelector('html')?.getAttribute('lang') || 'en';
                            const actionLabels = {
                                'en': '[aria-label="Mark as unread"]',
                                'ar': '[aria-label="تمييز كغير مقروءة"]',
                                'fr': '[aria-label="Marquer comme non lue"]'
                            };
                            const action = document.querySelector(actionLabels[lang]);
                            if (action) action.click();
                        }

                        const paneSide = document.getElementById('pane-side');
                        if (paneSide) paneSide.scrollTop = 0;

                        localStorage.setItem("ROOM_SELECTED", "true");
                        return;
                    } else {
                        item_pos++;
                    }
                }
                item.dispatchEvent(arrowDownEvent);
            }
        } catch (error) {
            console.error('Error in SELECT_ROOM:', error);
        }
    })""".trimIndent()

        val CHAT_ROOM_SCROLLER2 = """(async () => {
        try {
            await new Promise(resolve => setTimeout(resolve, 600));
            const root = document.querySelector('[id="main"]');
            if (!root) return;

            const room = root.querySelector('[role="application"]');
            if (!room) return;

            let prev_scroll = 0;
            let attempts = 0;
            const maxAttempts = 100; // Prevent infinite loop
            const timeout = 30000; // 30 seconds timeout

            const startTime = Date.now();
            while (attempts < maxAttempts && (Date.now() - startTime) < timeout) {
                const messageRows = room.querySelectorAll('div[role="row"]').length;
                const reloadButton = document.querySelector('[title="Load earlier messages…"]');
                if (reloadButton) {
                    await new Promise(resolve => setTimeout(resolve, 300));
                    reloadButton.click();
                    await new Promise(resolve => setTimeout(resolve, 300));
                }

                prev_scroll = room.parentElement.scrollTop;
                await new Promise(resolve => setTimeout(resolve, 5));
                room.parentElement.scrollTop -= 100; // Increased scroll distance
                await new Promise(resolve => setTimeout(resolve, 15));
                room.parentElement.scrollTop += 10;
                await new Promise(resolve => setTimeout(resolve, 15));

                if (prev_scroll === room.parentElement.scrollTop) break;
                attempts++;
            }

            await new Promise(resolve => setTimeout(resolve, 100));
            localStorage.setItem("DONE_LOADING_CHAT2", "true");
        } catch (error) {
            console.error('Error in CHAT_ROOM_SCROLLER2:', error);
        }
    })();
        """.trimIndent()

        val GET_CHAT = """
           (async () => {
               const getImageBase64 = async (imgElement) => {
                   return new Promise((resolve, reject) => {
                       try {
                           const canvas = document.createElement('canvas');
                           const ctx = canvas.getContext('2d');
                           
                           const img = new Image();
                           img.crossOrigin = 'Anonymous';
                           
                           img.onload = () => {
                               canvas.width = img.naturalWidth;
                               canvas.height = img.naturalHeight;
                               ctx.drawImage(img, 0, 0);
                               const dataURL = canvas.toDataURL('image/png');
                               const base64String = dataURL.split(',')[1];
                               resolve(base64String);
                           };
                           
                           img.onerror = () => reject(new Error('Failed to load image'));
                           img.src = imgElement.src;
                       } catch (error) {
                           reject(error);
                       }
                   });
               };

               try {
                   // Wait for page to settle
                   await new Promise(resolve => setTimeout(resolve, 1000));
                   
                   const root = document.querySelector('[id="main"]');
                   if (!root) throw new Error('Main element not found');
                   
                   root.click();
                   
                   const room = root.querySelector('[role="application"]');
                   if (!room) throw new Error('Application element not found');
                   
                   room.click();
                   
                   const messageList = [];
                   let messageRows = [];
                   
                   // Collect message rows
                   room.childNodes.forEach(node => {
                       if (node.nodeType === Node.ELEMENT_NODE) {
                           const rows = node.querySelectorAll('div[role="row"]');
                           if (rows.length > 0) {
                               messageRows = [...messageRows, ...rows];
                           } else {
                               messageRows = [...messageRows, node];
                           }
                       }
                   });

                   for (const row of messageRows) {
                       const messageData = {
                           id: '',
                           content: '',
                           timestamp: '',
                           image: '',
                           files: [],
                           voices: [],
                           reactions: [],
                           quotedMessage: null,
                           call: []
                       };

                       // Extract message ID
                       const messageElement = row.querySelector('div[data-id]');
                       if (messageElement) {
                           messageData.id = messageElement.getAttribute('data-id');
                       }

                       // Handle file messages
                       const file = row.querySelector('[title]');
                       const copyableTextElement = row.querySelector('.copyable-text');
                       const prePlainText = copyableTextElement ? copyableTextElement.getAttribute('data-pre-plain-text') || '' : '';

                       // Extract timestamp and sender
                       if (!file && prePlainText) {
                           const senderNameMatch = prePlainText.match(/\[(\d{1,2}:\d{2}), (\d{1,2}\/\d{1,2}\/\d{4})] (.+?):/);
                           if (senderNameMatch) {
                               messageData.timestamp = senderNameMatch[1];
                           }
                       }

                       // Extract message content
                       if (copyableTextElement) {
                           const innerCopyable = copyableTextElement.querySelector('.copyable-text');
                           messageData.content = innerCopyable ? innerCopyable.innerText : copyableTextElement.innerText;
                       }

                       // Extract quoted message
                       const quotedMessageElement = row.querySelector('div[aria-label="Quoted message"]');
                       if (quotedMessageElement) {
                           const quotedTextElement = quotedMessageElement.querySelector('.quoted-mention');
                           if (quotedTextElement) {
                               messageData.quotedMessage = { content: quotedTextElement.innerText };
                           }
                       }

                       // Handle images
                       const mediaElements = row.querySelectorAll('img');
                       if (mediaElements.length === 2) {
                           messageData.timestamp = row.innerText;
                           try {
                               const mediaUrl = mediaElements[1];
                               const base64Image = await getImageBase64(mediaUrl);
                               messageData.image = base64Image;
                           } catch (error) {
                               console.error('Error processing image:', error);
                               messageData.image = '';
                           }
                       }

                       // Handle voice messages
                       const voiceMessageElement = row.querySelector('[aria-label="Voice message"]');
                       if (voiceMessageElement) {
                           const item = row.querySelector('span[aria-label]');
                           if (item?.nextSibling?.lastChild) {
                               messageData.timestamp = item.nextSibling.lastChild.innerText;
                           }
                           const audioDurationElement = voiceMessageElement.parentElement?.innerText || '';
                           messageData.voices.push({ duration: audioDurationElement });
                       }

                       // Handle files
                       if (file) {
                           const fileContainer = file.querySelector('.x13faqbe');
                           const fileName = fileContainer?.innerText || '';
                           const metadataSpans = file.querySelectorAll('.x1rg5ohu[title]');
                           let pages = '', type = '', size = '';

                           if (file.parentElement?.lastChild) {
                               messageData.timestamp = file.parentElement.lastChild.innerText;
                           }

                           if (metadataSpans.length >= 2) {
                               if (metadataSpans[0].getAttribute('title').includes('page')) {
                                   pages = metadataSpans[0].innerText;
                                   type = metadataSpans[1].innerText;
                                   size = metadataSpans[2]?.innerText || '';
                               } else {
                                   type = metadataSpans[0].innerText;
                                   size = metadataSpans[1]?.innerText || '';
                               }
                           }

                           messageData.files.push({ fileName, pages, type, size });
                       }

                       // Handle reactions
                       const reactionButtons = row.querySelectorAll('button[aria-haspopup="true"]');
                       reactionButtons.forEach(button => {
                           const emoji = button.querySelector('img')?.alt || 'Reaction';
                           messageData.reactions.push({
                               emoji,
                               label: button.getAttribute('aria-label') || ''
                           });
                       });

                       // Handle calls
                       const checkCall = row.querySelector('button');
                       if (checkCall) {
                           messageData.call = row.innerText.split('\n');
                           messageData.timestamp = messageData.call.pop() || '';
                       }

                       // Add valid messages to the list
                       if (messageData.call.length < 3 && (
                           messageData.content ||
                           messageData.timestamp ||
                           messageData.image ||
                           messageData.files.length ||
                           messageData.voices.length
                       )) {
                           messageList.push(messageData);
                       } else {
                           const span = row.firstElementChild?.querySelector('span');
                           if (span?.innerText) {
                               messageData.content = span.innerText;
                               messageList.push(messageData);
                           }
                       }
                   }

                   // Save to localStorage
                   await new Promise(resolve => setTimeout(resolve, 1000));
                   localStorage.setItem('CHATS', JSON.stringify(messageList));
               } catch (error) {
                   console.error('Error processing messages:', error);
               }
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

    }
}