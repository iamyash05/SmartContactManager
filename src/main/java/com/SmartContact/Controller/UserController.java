package com.SmartContact.Controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.SmartContact.Helper.Message;
import com.SmartContact.Model.Contact;
import com.SmartContact.Model.User;
import com.SmartContact.Repository.ContactRepo;
import com.SmartContact.Repository.UserRepo;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepo userrepo;
	
	@Autowired
	private ContactRepo contrepo;
	
	@ModelAttribute
	public void addCommonData(Model m, Principal p) {
		String name = p.getName();
		User user = userrepo.getUserByUserName(name);
		m.addAttribute("user", user);
	}
	
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, HttpSession session,
			@RequestParam("profileImage") MultipartFile file, Principal principal) {
		try {
			String name = principal.getName();
			User user = this.userrepo.getUserByUserName(name);
			contact.setUser(user);
			
			if(file.isEmpty()) {
				contact.setImageUrl("contact.png");
			} else {
				contact.setImageUrl(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			user.getContacts().add(contact);
			
			
			this.userrepo.save(user);
			
			session.setAttribute("message", new Message("Your Contact Is Added Successfully", "success"));
			
		}catch(Exception e) {
			System.out.println("ERROR "+e.getMessage());
			session.setAttribute("message", new Message("Something went wrong", "danger"));
		}
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show All Contacts");
		String name = principal.getName();
		
		User user = this.userrepo.getUserByUserName(name);
		
		Pageable pageable =  PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contrepo.findContactByUser(user.getId(), pageable);
		
		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}
	
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, 
			Model model, Principal principal) {
		Optional<Contact> contactOp =  contrepo.findById(cId);
		Contact contact = contactOp.get();
		
		String userName = principal.getName();
		User user = this.userrepo.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId,Principal principal, 
			HttpSession http) {
		
		Optional<Contact> contactOp = this.contrepo.findById(cId);
		
		Contact contact = contactOp.get();
		
		String name = principal.getName();
		
		User user = this.userrepo.getUserByUserName(name);
		
		//contact.setUser(null);
		//user.getContacts().remove(contact);
		
		if(user.getId() == contact.getUser().getId()) {
			user.getContacts().remove(contact);
			contact.setUser(null);
			this.contrepo.delete(contact);
			http.setAttribute("message", new Message("Contact Deleted Successfully...", "success"));
			File f = new File("static/img/"+contact.getImageUrl());
			f.delete();
			this.userrepo.save(user);
		}
		
		return "redirect:/user/show_contacts/0";
	}
	
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contrepo.findById(cId).get();
		
		model.addAttribute("contact",contact);
		
		return "/normal/update_form";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, 
			@RequestParam("profileImage") MultipartFile file, Model model, 
			HttpSession session, Principal principal) {
		try {
			Contact oldContact = this.contrepo.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				
				File deleteFile = new ClassPathResource("/static/img").getFile();
				File file1 = new File(deleteFile, oldContact.getImageUrl());
				file1.delete();
				
				File saveFile = new ClassPathResource("/static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),  path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImageUrl(file.getOriginalFilename());
			} else {
				contact.setImageUrl(oldContact.getImageUrl());
			}
			
			User user = this.userrepo.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contrepo.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is updated", "success"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
}