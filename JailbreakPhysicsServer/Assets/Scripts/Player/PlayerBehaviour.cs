using System;
using UnityEngine;

public class PlayerBehaviour : MonoBehaviour
{
    public short playerID;
    public string playerName;
    public Rigidbody Rigidbody { get; set; }

    [Header("Camera Input")] 
    public Vector3 cameraPosition;
    public Quaternion cameraRotation;
    public float offsetZ;
    
    [Header("Movement Settings")] 
    public float jumpDownDelay = 0.5f;
    public float walkSpeed = 6f;
    public float sprintSpeed = 9f;
    public float jumpForce = 300f;
    public float rotationSpeed = 10f;
    public float groundCheckDistance = 1.1f;
    public LayerMask groundLayer;

    [Header("Input Values")] 
    public float horizontalInput;
    public float verticalInput;
    
    public bool jumpInputDown;
    public bool sprintInputDown;

    public float jumpDownDelayTime;
    public float sprintDownTime;
    
    [Header("Runtime")] 
    public bool isGrounded;
    public float notGroundedTime;
    
    void Start()
    {
        Rigidbody = GetComponent<Rigidbody>();
        Rigidbody.freezeRotation = true;
    }

    void Update()
    {
        if (sprintInputDown)
            sprintDownTime += Time.deltaTime;
        else
            sprintDownTime = 0;
        
        if (jumpDownDelayTime > 0)
            jumpDownDelayTime -= Time.deltaTime;
        
        CheckGroundStatus();
        UpdateMovement();
        HandleJump();
    }

    public void UpdatePlayerInput(
        Vector3 cameraPos, Quaternion cameraRot, float offsetZ,
        float horizontalInput, float verticalInput, bool jumpDown, bool sprintDown)
    {
        this.cameraPosition = cameraPos;
        this.cameraRotation = cameraRot;
        this.offsetZ = offsetZ;
        this.horizontalInput = horizontalInput;
        this.verticalInput = verticalInput;
        this.jumpInputDown = jumpDown;
        this.sprintInputDown = sprintDown;
    }

    private void CheckGroundStatus()
    {
        isGrounded = Physics.Raycast(transform.position + Vector3.up * 0.1f, Vector3.down, groundCheckDistance + 0.1f, groundLayer);

        if (!isGrounded)
        {
            notGroundedTime += Time.fixedDeltaTime;
        }
        else
        {
            notGroundedTime = 0f;
        }
    }

    private void UpdateMovement()
    {
        Vector3 cameraForward = cameraRotation * Vector3.forward;
        Vector3 cameraRight = cameraRotation * Vector3.right;
        
        cameraForward.y = 0; // Игнорируем вертикальную составляющую камеры
        cameraRight.y = 0;
        cameraForward.Normalize();
        cameraRight.Normalize();

        Vector3 moveDirection = cameraForward * verticalInput + cameraRight * horizontalInput;
        moveDirection.Normalize(); // Нормализуем, чтобы диагональное движение не было быстрее

        float currentSpeed = sprintDownTime != 0 ? sprintSpeed : walkSpeed;
        
        // Применяем силу к Rigidbody для движения
        Vector3 targetVelocity = moveDirection * currentSpeed;
        targetVelocity.y = Rigidbody.linearVelocity.y; // Сохраняем вертикальную скорость (гравитацию, прыжок)

        Rigidbody.linearVelocity = Vector3.Lerp(Rigidbody.linearVelocity, targetVelocity, Time.fixedDeltaTime * 10f); // Плавное изменение скорости

        // Вращение персонажа в направлении движения
        if (moveDirection.magnitude > 0.1f) // Проверяем, есть ли движение, чтобы избежать дерганий при 0
        {
            Quaternion targetRotation = Quaternion.LookRotation(moveDirection);
            Rigidbody.rotation = Quaternion.Slerp(Rigidbody.rotation, targetRotation, rotationSpeed * Time.fixedDeltaTime);
        }
    }
    
    private void HandleJump()
    {
        if (jumpInputDown && jumpDownDelayTime <= 0 && isGrounded)
        {
            Rigidbody.AddForce(Vector3.up * jumpForce, ForceMode.Impulse);
            jumpDownDelayTime = jumpDownDelay;
        }
    }
    
    

    public GameObject getGameObject()
    {
        return this.gameObject;
    }

    public Rigidbody getRigidbody()
    {
        return this.Rigidbody;
    }
}
