import {test,expect} from '@playwright/test';

test('real loopback service preserves v2 fields across editor save and independent remote edit',async({page,request})=>{
 const base='http://127.0.0.1:8767/profile-library/';
 const endpoint=base+'api/v1/catalog',headers={Authorization:'Bearer preview-only'};
 const errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.goto(base);await page.getByLabel('Library key',{exact:true}).fill('incorrect-preview-token');await page.locator('#load').click();await expect(page.locator('#status')).toContainText('not accepted');
 await page.getByLabel('Library key',{exact:true}).fill('preview-only');await page.locator('#load').click();await expect(page.locator('#status')).toContainText('Connected');
 await page.locator('.sidebar-footer [data-open-dialog="import-dialog"]').click();await page.locator('#load-adaptive').click();await expect(page.locator('.phase-row')).toHaveCount(6);
 await page.locator('#save').click();await expect(page.locator('#editor-status')).toContainText('Saved');
 const catalog=await (await request.get(endpoint,{headers})).json();expect(catalog.schemaVersion).toBe(2);expect(catalog.profiles).toHaveLength(1);
 const originalProgram=structuredClone(catalog.profiles[0].profile.program);expect(originalProgram.recipe.source.original.steps).toHaveLength(7);expect(originalProgram.phases.at(-1).ramp.source.capture).toBe('extractionStartFlow');
 await page.locator('#editor summary[aria-label="Profile actions"]').click();await page.getByRole('button',{name:'Recipe settings',exact:true}).click();await page.locator('#profile-description').fill('Local independent note');await page.locator('#recipe-dialog').getByRole('button',{name:'Done',exact:true}).click();
 catalog.profiles[0].profile.name='Remote API rename';catalog.profiles[0].favorite=true;
 const remote=await request.put(endpoint,{headers:{...headers,'If-Match':String(catalog.revision)},data:catalog});expect(remote.status()).toBe(200);
 await page.locator('#save').click();await expect(page.locator('#save')).toBeDisabled();await page.locator('#use-latest').click();
 await expect(page.locator('[data-path="profile.name"]')).toHaveValue('Remote API rename');await expect(page.locator('[data-path="favorite"]')).toBeChecked();await expect(page.locator('#profile-description')).toHaveValue('Local independent note');
 await page.locator('#save').click();await expect(page.locator('#editor-status')).toContainText('Saved');
 const final=await (await request.get(endpoint,{headers})).json();expect(final.revision).toBe(3);expect(final.profiles[0].profile.program).toEqual(originalProgram);expect(final.profiles[0].favorite).toBe(true);expect(final.profiles[0].profile.description).toBe('Local independent note');expect(errors).toEqual([]);
});
