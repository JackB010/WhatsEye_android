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
        val CURRENT_CHATS = """(  ()=>{
               const getContacts = async() =>{
               if (document.querySelectorAll('[role="listitem"]').length === 0)
                  return []
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
               contacts = []
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name) {
                     name = name.innerText;
                     const timestamp = item.querySelector("div[role='gridcell']").lastChild.innerText;
                     let last_msg = item.querySelector("div[role='gridcell']").parentElement.childNodes[1].innerText
                     let num_unread = item.querySelector('[role="gridcell"][aria-colindex="1"]').innerText
                     let icon = item.querySelector('img') ? item.querySelector('img').src : "";
                       //contacts.push({ name, timestamp,num_unread ,last_msg, icon });
                            contacts.push({ name});
                     }
                        document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
                item = document.querySelectorAll('[aria-selected="true"]')[1]
                if(item.querySelector('[aria-label="Unread"]')){
                    await new Promise(resolve => setTimeout(resolve,    300));
                    item.querySelector('[aria-label="Open the chat context menu"]').click()
                    await new Promise(resolve => setTimeout(resolve,    300));
                    document.querySelector('[aria-label="Mark as unread"]').click()
                 
                }
                document.getElementById("pane-side").scrollTop = 0;
                return contacts
               }
               getContacts().then(data =>{ localStorage.setItem("CURRENT_CHATS", JSON.stringify(data))} )              
            })();""".trimIndent()

        val SELECT_ROOM = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('div[aria-label="Search input textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-label="Chat list"]').getAttribute('aria-rowcount')
               let item_pos=0;
               for(let i=0; i<nb; i++){
                  await new Promise(resolve => setTimeout(resolve, 60));
                  item = document.querySelectorAll('[aria-selected="true"]')[1]
                  let name = item.querySelector("span[title]");
                  if (name ) {
                     name = name.innerText;
                     if (search === name){
                        if (item_pos===pos ) {
                        if(item.querySelector('[aria-label="Unread"]')){ 
                            await new Promise(resolve => setTimeout(resolve,    300));
                            item.querySelector('[aria-label="Open the chat context menu"]').click()
                            await new Promise(resolve => setTimeout(resolve,    1000));
                            document.querySelector('[aria-label="Mark as unread"]').click() 
                        }
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
            })""".trimIndent()

        val CHAT_ROOM_SCROLLER = """(async()=>{
                   const root = document.querySelector('[id="main"]')
                   const room = root.querySelector('[role="application"]')
                   let prev_scroll = 0
                   while (prev_scroll != room.parentElement.scrollTop){
                       prev_scroll = room.parentElement.scrollTop
                       room.parentElement.scrollTop -= 500
                       await new Promise(resolve => setTimeout(resolve, 60));
                   }
        })();
        """.trimIndent()

        val GET_CHAT = """(()=> {
                        const messageList = []; 
                        const messageRows = document.querySelectorAll('div[role="row"]'); // Select all rows containing messages.
                    
                        messageRows.forEach(row => {
                            const messageData = {
                                id: '',
                                sender: '',
                                content: '',
                                timestamp: '',
                                images: '',
                                files: [],
                                voices: [],
                                reactions: [],
                                quotedMessage: null // Initialize quoted message field
                            };
                    
                            // Fetch message ID from the data-id attribute of the message container
                            const messageElement = row.querySelector('div[data-id]');
                            if (messageElement) {
                                messageData.id = messageElement.getAttribute('data-id'); // Extract message ID
                            }
                    
                            // Find the sender and timestamp
                            const copyableTextElement = row.querySelector('.copyable-text');
                            const prePlainText = copyableTextElement ? copyableTextElement.getAttribute('data-pre-plain-text') : '';
                            const senderNameMatch = prePlainText.match(/\[(\d{1,2}:\d{2}), (\d{1,2}\/\d{1,2}\/\d{4})] (.+?):/);
                    
                            if (senderNameMatch) {
                                messageData.timestamp = senderNameMatch[1]; // Extract timestamp
                                messageData.sender = senderNameMatch[3]; // Extract sender's name
                            }
                    
                            // Extract main message content.
                            if (copyableTextElement) {
                                if (copyableTextElement.childElementCount===1)
                                    messageData.content = copyableTextElement.innerText;
                                else
                                    messageData.content = copyableTextElement.querySelector('.copyable-text').innerText;
                    
                            }
                    
                            // Find any quoted message.
                            const quotedMessageElement = row.querySelector('div[aria-label="Quoted message"]');
                            if (quotedMessageElement) {
                                const quotedTextElement = quotedMessageElement.querySelector('.quoted-mention');
                                if (quotedTextElement) {
                                    messageData.quotedMessage = {
                                        content: quotedTextElement.innerText
                                    };
                                }
                            }
                    
                            // Extract media (images and documents).
                            const mediaElements = row.querySelectorAll('div[role="button"] img');
                            mediaElements.forEach(img => {
                                const mediaUrl = img.src || img.getAttribute('data-src'); // Use data-src if available.
                                if (mediaUrl) {
                                    messageData.image = mediaUrl // Capture the image UR
                                }
                            });
                    
                            // Extract voice messages.
                            const voiceMessageElement = row.querySelector('[aria-label="Voice message"]');
                    
                            if (voiceMessageElement) {
                                item = row.querySelector('span[aria-label]')
                                messageData.sender = item.getAttribute('aria-label').replace(':','')
                                item = item.nextSibling
                                messageData.timestamp = item.lastChild.innerText
                                const audioDurationElement = voiceMessageElement.parentElement.innerText
                    
                                // Link the sender to the voice message
                                messageData.voices.push({
                                    duration: audioDurationElement,
                                });
                            }
                            // Etract file messages.
                            const file = row.querySelector('[title]')
                            if(file){
                                if(file.querySelector('[title]')){
                                item = row.querySelector('span[aria-label]')
                                messageData.sender = item.getAttribute('aria-label').replace(':','')
                                messageData.timestamp = item.nextSibling.innerText
                                const fileName = file.querySelector('[title]').parentElement.previousElementSibling.innerText
                                const pages = file.querySelectorAll('[title]')[0].innerText
                                const type = file.querySelectorAll('[title]')[1].innerText
                                const size = file.querySelectorAll('[title]')[2].innerText
                                 messageData.files.push({
                                    fileName,
                                    pages,
                                    type,
                                    size
                                });
                             }
                            } 
                            // Extract reactions.
                            const reactionButtons = row.querySelectorAll('button[aria-haspopup="true"]');
                            reactionButtons.forEach(button => {
                                const emoji = button.querySelector('img') ? button.querySelector('img').alt : 'Reaction';
                                messageData.reactions.push({
                                    emoji,
                                    label: button.getAttribute('aria-label') // Get the aria-label text.
                                });
                            });
                    
                            // Add the message data to the message list if it has any content.
                                messageList.push(messageData);
                        });
                    
                        // Return the extracted messages in JSON format.
                        return JSON.stringify(messageList, null, 2);
                    })();""".trimIndent()

        val BLOCK_USER = """(async (search, pos=0)=>{
               if (document.querySelectorAll('[role="listitem"]').length === 0) return []
               const arrowDownEvent = new KeyboardEvent('keydown', {
                        key: 'ArrowDown',
                        code: 'ArrowDown',
                        keyCode: 40,
                        which: 40,
                        bubbles: true
                    });
               const searchBox = document.querySelector('div[aria-label="Search input textbox"]');
               searchBox.focus();
               searchBox.dispatchEvent(arrowDownEvent);
               const nb = document.querySelector('[aria-label="Chat list"]').getAttribute('aria-rowcount')
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
                            item.querySelector('[aria-label="Open the chat context menu"]').click()
                            await new Promise(resolve => setTimeout(resolve, 300));
                            let action = document.querySelector('[aria-label="Block"]')
                            let block = true

                            if(!action){
                                action = document.querySelector('[aria-label="Exit group"]')
                                block = false
                            }
                            if(action){
                                action.click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                            }else block = true

                            if(block){
                                await new Promise(resolve => setTimeout(resolve, 300));
                                item.querySelector('[aria-label="Open the chat context menu"]').click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                document.querySelector('[aria-label="Delete chat"]').click()
                                await new Promise(resolve => setTimeout(resolve, 300));
                                action = document.querySelector('[role="dialog"]').querySelectorAll('button')
                                action[action.length -1].click()
                            }
                            return;
                        }else {item_pos +=1} 
                        }
                     }
                  document.querySelectorAll('[aria-selected="true"]')[1].dispatchEvent(arrowDownEvent);
                  }
            })""".trimIndent()
    }
}